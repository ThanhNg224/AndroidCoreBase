package com.thanhng224.androidcorebase.core.storage.database

import com.thanhng224.androidcorebase.core.storage.secure.SecureStore
import com.thanhng224.androidcorebase.core.storage.secure.SecureStoreKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves (and memoizes) the SQLCipher passphrase. `DbPassphraseWarmupInitializer` warms this
 * on a background dispatcher during process startup so `DatabaseModule`'s Hilt `@Provides`
 * boundary — which must stay synchronous — almost always hits the cached value instead of
 * blocking on encrypted-prefs disk I/O.
 */
@Singleton
internal class DbPassphraseProvider
    @Inject
    internal constructor(
        private val secureStore: SecureStore,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var cached: String? = null

        suspend fun getOrCreate(): String {
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

        companion object {
            private val DB_PASSPHRASE_KEY = SecureStoreKey("db_passphrase")
        }
    }
