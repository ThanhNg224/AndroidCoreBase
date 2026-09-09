package com.thanhng224.androidcorebase.core.storage.secure

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encodes/decodes the encrypted-file secure store's binary envelope:
 *
 * ```
 * 4 bytes  magic "ACB2"
 * 1 byte   format version
 * 1 byte   IV length
 * N bytes  random IV
 * ...      AES-GCM ciphertext + 128-bit tag
 * ```
 *
 * The plaintext is a sorted JSON object of string key/value pairs, encrypted with a non-exportable
 * AES-256-GCM Android Keystore key. Any structural or cryptographic failure (bad magic/version,
 * truncated payload, altered tag, malformed JSON) is treated as an absent store -- never logged,
 * never crashes the caller.
 */
internal object EncryptedFileCodec {
    private val MAGIC = byteArrayOf('A'.code.toByte(), 'C'.code.toByte(), 'B'.code.toByte(), '2'.code.toByte())
    private const val FORMAT_VERSION: Byte = 1
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "core_secure_store_key"
    private const val KEY_SIZE_BITS = 256
    private val HEADER_SIZE_BYTES = MAGIC.size + 1 + 1 // magic + version + ivLength

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(values: Map<String, String>): ByteArray {
        // Pin the type argument to Map<String, String>: encodeToString<T> resolves its
        // serializer from the compile-time type, and kotlinx.serialization has no direct
        // serializer for SortedMap/TreeMap (only for Map), so leaving T inferred from
        // toSortedMap()'s SortedMap return type sends it down the polymorphic-serializer path,
        // which then fails because TreeMap isn't @Serializable.
        val plaintext = json.encodeToString<Map<String, String>>(values.toSortedMap())
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return MAGIC + byteArrayOf(FORMAT_VERSION, iv.size.toByte()) + iv + ciphertext
    }

    /** Null for any malformed or tampered payload; never throws. */
    fun decodeOrNull(bytes: ByteArray): Map<String, String>? {
        if (bytes.size < HEADER_SIZE_BYTES) return null
        if (!bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
        if (bytes[MAGIC.size] != FORMAT_VERSION) return null
        val ivLength = bytes[MAGIC.size + 1].toInt() and 0xFF
        val ivStart = HEADER_SIZE_BYTES
        val ciphertextStart = ivStart + ivLength
        if (ivLength <= 0 || bytes.size <= ciphertextStart) return null

        val iv = bytes.copyOfRange(ivStart, ciphertextStart)
        val ciphertext = bytes.copyOfRange(ciphertextStart, bytes.size)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plaintext = cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
            json.decodeFromString<Map<String, String>>(plaintext)
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: SerializationException) {
            null
        }
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
}
