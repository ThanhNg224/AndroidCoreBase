package com.thanhng224.androidcorebase.core.storage.secure

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies a stable, randomly generated passphrase for an encrypted database, persisted through
 * [SecureStore] (so the key material itself sits behind the Keystore) and memoized in memory after
 * the first read.
 *
 * `:core` deliberately ships **no** database. Room's `@Database` fixes its `entities` list at
 * compile time in the annotated class, so a library cannot hand consumers a database to extend —
 * every app declares its own. What *is* reusable is this: generate the passphrase once, never
 * again, and never keep it in plaintext. Wire it into your own SQLCipher `SupportFactory`:
 *
 * ```kotlin
 * @Provides
 * @Singleton
 * fun provideDatabase(
 *     @ApplicationContext context: Context,
 *     passphraseProvider: DbPassphraseProvider,
 * ): MyDatabase {
 *     val passphrase = runBlocking { passphraseProvider.getOrCreate() }
 *     return Room.databaseBuilder(context, MyDatabase::class.java, "my_database.db")
 *         .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray()))
 *         .build()
 * }
 * ```
 *
 * Note the `runBlocking` there: [getOrCreate] is `suspend` because the first call reads encrypted
 * storage from disk, while a Hilt `@Provides` boundary is synchronous. Blocking is tolerable
 * because it happens once and Room builds lazily on first query — but to keep it off the critical
 * path entirely, warm it during startup from your own `androidx.startup` `Initializer` or
 * `Application.onCreate` on a background dispatcher, so the `@Provides` call hits the memoized
 * value. `:core` used to ship exactly such an initializer and register it unconditionally, which
 * charged every consuming app Keystore I/O at every process start even with no database at all;
 * that is now the consumer's call to make (see `docs/MODERNIZATION.md` F7 and D5).
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
