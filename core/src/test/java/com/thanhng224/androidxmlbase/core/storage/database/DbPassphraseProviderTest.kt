package com.thanhng224.androidxmlbase.core.storage.database

import com.thanhng224.androidxmlbase.core.storage.secure.SecureStore
import com.thanhng224.androidxmlbase.core.storage.secure.SecureStoreKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DbPassphraseProviderTest {
    @Test
    fun `generates and persists a new passphrase when none exists`() =
        runTest {
            val secureStore = FakeSecureStore()
            val provider = DbPassphraseProvider(secureStore)

            val passphrase = provider.getOrCreate()

            assertNotNull(passphrase)
            assertEquals(passphrase, secureStore.stored["db_passphrase"])
        }

    @Test
    fun `reuses the existing passphrase instead of generating a new one`() =
        runTest {
            val secureStore = FakeSecureStore().apply { stored["db_passphrase"] = "existing-key" }
            val provider = DbPassphraseProvider(secureStore)

            val passphrase = provider.getOrCreate()

            assertEquals("existing-key", passphrase)
        }

    @Test
    fun `caches the passphrase so a second call does not read the store again`() =
        runTest {
            val secureStore = FakeSecureStore()
            val provider = DbPassphraseProvider(secureStore)

            val first = provider.getOrCreate()
            secureStore.stored.clear()
            val second = provider.getOrCreate()

            assertEquals(first, second)
        }

    private class FakeSecureStore : SecureStore {
        val stored = mutableMapOf<String, String>()

        override suspend fun getString(key: SecureStoreKey): String? = stored[key.name]

        override suspend fun putString(
            key: SecureStoreKey,
            value: String,
        ) {
            stored[key.name] = value
        }

        override suspend fun remove(key: SecureStoreKey) {
            stored.remove(key.name)
        }

        override suspend fun clear() {
            stored.clear()
        }
    }
}
