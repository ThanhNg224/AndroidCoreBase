package com.thanhng224.androidcorebase.core.storage.secure

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and provides a stable passphrase for database encryption (e.g. SQLCipher),
 * persisted via [SecureStore] and memoized in memory.
 */
@Singleton
public class DbPassphraseProvider
    @Inject
    public constructor(
        private val secureStore: SecureStore,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var cached: String? = null

        public suspend fun getOrCreate(): String {
            cached?.let { return it }
            return mutex.withLock {
                cached?.let { return@withLock it }
                val existing = secureStore.getString(DB_PASSPHRASE_KEY)
                val passphrase =
                    existing?.takeIf { it.isNotEmpty() }
                        ?: UUID.randomUUID().toString().also { secureStore.putString(DB_PASSPHRASE_KEY, it) }
                cached = passphrase
                passphrase
            }
        }

        private companion object {
            val DB_PASSPHRASE_KEY = SecureStoreKey("db_passphrase")
        }
    }
