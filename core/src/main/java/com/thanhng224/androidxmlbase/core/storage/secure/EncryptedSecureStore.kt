package com.thanhng224.androidxmlbase.core.storage.secure

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.thanhng224.androidxmlbase.core.architecture.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/**
 * [SecureStore] backed by a Keystore-resident AES-256/GCM key: `EncryptedSharedPreferences`
 * (androidx.security:security-crypto) is deprecated as of 1.1.0 with no replacement class, and
 * Google's guidance is to use the platform Keystore directly instead. The key never leaves the
 * TEE/StrongBox, so only ciphertext (IV-prefixed, Base64-encoded) is ever written to disk.
 */
internal class EncryptedSecureStore
    @Inject
    internal constructor(
        @param:ApplicationContext private val context: Context,
        private val dispatchers: AppDispatchers,
    ) : SecureStore {
        private val preferences: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }

        override suspend fun getString(key: SecureStoreKey): String? =
            withContext(dispatchers.io) {
                preferences.getString(key.name, null)?.let(::decryptOrNull)
            }

        override suspend fun putString(
            key: SecureStoreKey,
            value: String,
        ) {
            withContext(dispatchers.io) {
                preferences.edit(commit = true) {
                    putString(key.name, encrypt(value))
                }
            }
        }

        override suspend fun remove(key: SecureStoreKey) {
            withContext(dispatchers.io) {
                preferences.edit(commit = true) {
                    remove(key.name)
                }
            }
        }

        override suspend fun clear() {
            withContext(dispatchers.io) {
                preferences.edit(commit = true) {
                    clear()
                }
            }
        }

        private fun encrypt(plaintext: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
        }

        private fun decryptOrNull(encoded: String): String? =
            try {
                val payload = Base64.decode(encoded, Base64.NO_WRAP)
                val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
                val ciphertext = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            } catch (e: GeneralSecurityException) {
                Timber.w(e, "Failed to decrypt secure store value for key; treating as absent")
                null
            }

        private fun secretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            val keySpec =
                KeyGenParameterSpec
                    .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            keyGenerator.init(keySpec)
            return keyGenerator.generateKey()
        }

        private companion object {
            const val FILE_NAME = "secure_store"
            const val ANDROID_KEY_STORE = "AndroidKeyStore"
            const val KEY_ALIAS = "androidxmlbase_secure_store_key"
            const val KEY_SIZE_BITS = 256
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val GCM_IV_LENGTH_BYTES = 12
            const val GCM_TAG_LENGTH_BITS = 128
        }
    }
