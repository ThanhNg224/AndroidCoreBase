package com.thanhng224.androidcorebase.core.storage.secure

import com.thanhng224.androidcorebase.core.testing.FakeSecureStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Base64

class DbPassphraseProviderTest {
    @Test
    fun `generates and persists a new 32-byte passphrase when none exists`() =
        runTest {
            val secureStore = FakeSecureStore()
            val provider = DbPassphraseProvider(secureStore)

            val passphrase = provider.getOrCreate()

            assertEquals(32, passphrase.size)
            val persisted = Base64.getDecoder().decode(secureStore.stored["db_passphrase"])
            assertArrayEquals(passphrase, persisted)
        }

    @Test
    fun `reuses the existing passphrase instead of generating a new one`() =
        runTest {
            val existing = ByteArray(32) { it.toByte() }
            val secureStore =
                FakeSecureStore().apply {
                    stored["db_passphrase"] = Base64.getEncoder().encodeToString(existing)
                }
            val provider = DbPassphraseProvider(secureStore)

            val passphrase = provider.getOrCreate()

            assertArrayEquals(existing, passphrase)
        }

    @Test
    fun `caches the passphrase so a second call does not read the store again`() =
        runTest {
            val secureStore = FakeSecureStore()
            val provider = DbPassphraseProvider(secureStore)

            val first = provider.getOrCreate()
            secureStore.stored.clear()
            val second = provider.getOrCreate()

            assertArrayEquals(first, second)
        }

    @Test
    fun `regenerates the passphrase when the stored value is not valid base64`() =
        runTest {
            val secureStore = FakeSecureStore().apply { stored["db_passphrase"] = "not-valid-base64!!" }
            val provider = DbPassphraseProvider(secureStore)

            val passphrase = provider.getOrCreate()

            assertEquals(32, passphrase.size)
            assertNotEquals("not-valid-base64!!", secureStore.stored["db_passphrase"])
        }

    @Test
    fun `regenerates the passphrase when the stored value decodes to the wrong length`() =
        runTest {
            val wrongLength = Base64.getEncoder().encodeToString(ByteArray(16))
            val secureStore = FakeSecureStore().apply { stored["db_passphrase"] = wrongLength }
            val provider = DbPassphraseProvider(secureStore)

            val passphrase = provider.getOrCreate()

            assertEquals(32, passphrase.size)
            assertNotEquals(wrongLength, secureStore.stored["db_passphrase"])
        }
}
