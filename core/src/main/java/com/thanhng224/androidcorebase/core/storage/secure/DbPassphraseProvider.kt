package com.thanhng224.androidcorebase.core.storage.secure

import com.thanhng224.androidcorebase.core.foundation.SecureStore
import com.thanhng224.androidcorebase.core.foundation.SecureStoreKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import kotlin.io.encoding.Base64

/**
 * Generates and provides a stable 32-byte passphrase for database encryption (e.g. SQLCipher),
 * persisted Base64-encoded via [SecureStore] (which is string-valued) and memoized in memory. A
 * stored value that fails to decode, or does not decode to exactly 32 bytes, is treated as absent
 * and atomically replaced with a freshly generated passphrase.
 */
public class DbPassphraseProvider
    public constructor(
        private val secureStore: SecureStore,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var cached: ByteArray? = null

        public suspend fun getOrCreate(): ByteArray {
            cached?.let { return it }
            return mutex.withLock {
                cached?.let { return@withLock it }
                val passphrase = loadValidOrNull() ?: generateAndPersist()
                cached = passphrase
                passphrase
            }
        }

        private suspend fun loadValidOrNull(): ByteArray? {
            val stored = secureStore.getString(DB_PASSPHRASE_KEY) ?: return null
            val decoded =
                try {
                    Base64.Default.decode(stored)
                } catch (_: IllegalArgumentException) {
                    null
                }
            return decoded?.takeIf { it.size == PASSPHRASE_LENGTH_BYTES }
        }

        private suspend fun generateAndPersist(): ByteArray {
            val passphrase = newPassphrase()
            secureStore.putString(DB_PASSPHRASE_KEY, Base64.Default.encode(passphrase))
            return passphrase
        }

        private fun newPassphrase(): ByteArray = ByteArray(PASSPHRASE_LENGTH_BYTES).also(SecureRandom()::nextBytes)

        private companion object {
            val DB_PASSPHRASE_KEY = SecureStoreKey("db_passphrase")

            // const val JVM fields are static regardless of the companion's own visibility, so
            // this needs its own `private` -- the enclosing `private companion object` alone
            // does not make a const val private.
            private const val PASSPHRASE_LENGTH_BYTES = 32
        }
    }
