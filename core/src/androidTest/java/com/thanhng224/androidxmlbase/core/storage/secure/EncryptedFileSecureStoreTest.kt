package com.thanhng224.androidcorebase.core.storage.secure

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanhng224.androidcorebase.core.architecture.DefaultAppDispatchers
import com.thanhng224.androidcorebase.core.foundation.SecureStoreKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class EncryptedFileSecureStoreTest {
    private lateinit var context: Context
    private lateinit var storeFile: File
    private lateinit var store: EncryptedFileSecureStore
    private val key = SecureStoreKey("test_key")

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        storeFile = File(context.noBackupFilesDir, STORE_FILE_NAME)
        storeFile.delete()
        storeFile.parentFile?.setWritable(true)
        store = EncryptedFileSecureStore(context, DefaultAppDispatchers())
    }

    @After
    fun tearDown() {
        storeFile.parentFile?.setWritable(true)
        storeFile.delete()
    }

    @Test
    fun putString_thenGetString_returnsOriginalPlaintext() =
        runBlocking {
            store.putString(key, "super-secret-value")

            assertEquals("super-secret-value", store.getString(key))
        }

    @Test
    fun getString_withoutPriorPut_returnsNull() =
        runBlocking {
            assertNull(store.getString(key))
        }

    @Test
    fun storeFile_livesUnderNoBackupFilesDir() =
        runBlocking {
            store.putString(key, "super-secret-value")

            assertTrue(storeFile.canonicalPath.startsWith(context.noBackupFilesDir.canonicalPath))
            assertTrue(storeFile.exists())
        }

    @Test
    fun storedValue_isNotPlaintextOnDisk() =
        runBlocking {
            store.putString(key, "super-secret-value")

            val rawOnDisk = storeFile.readBytes().toString(Charsets.UTF_8)

            assertFalse(rawOnDisk.contains("super-secret-value"))
        }

    @Test
    fun remove_clearsStoredValue() =
        runBlocking {
            store.putString(key, "super-secret-value")

            store.remove(key)

            assertNull(store.getString(key))
        }

    @Test
    fun clear_removesAllStoredValues() =
        runBlocking {
            val otherKey = SecureStoreKey("other_key")
            store.putString(key, "value-one")
            store.putString(otherKey, "value-two")

            store.clear()

            assertNull(store.getString(key))
            assertNull(store.getString(otherKey))
        }

    @Test
    fun truncatedPayload_isTreatedAsAbsentWithoutCrashing() =
        runBlocking {
            store.putString(key, "super-secret-value")
            val fullBytes = storeFile.readBytes()
            storeFile.writeBytes(fullBytes.copyOf(fullBytes.size / 2))

            assertNull(store.getString(key))
        }

    @Test
    fun alteredGcmTag_isTreatedAsAbsentWithoutCrashing() =
        runBlocking {
            store.putString(key, "super-secret-value")
            val bytes = storeFile.readBytes()
            val corrupted = bytes.copyOf()
            corrupted[corrupted.size - 1] = (corrupted[corrupted.size - 1].toInt() xor 0xFF).toByte()
            storeFile.writeBytes(corrupted)

            assertNull(store.getString(key))
        }

    @Test
    fun cancellationBeforeAWriteApplies_leavesThePreviousValueIntact() =
        runBlocking {
            store.putString(key, "committed-value")

            // Cancelling right after launch, before this single-threaded scope ever dispatches
            // the child, guarantees the write body never starts.
            val deferred = async { store.putString(key, "value-that-must-not-persist") }
            deferred.cancel()

            var caughtCancellation = false
            try {
                deferred.await()
            } catch (e: CancellationException) {
                caughtCancellation = true
            }

            assertTrue(caughtCancellation)
            assertEquals("committed-value", store.getString(key))
        }

    @Test
    fun writeFailure_leavesThePreviousCommittedValueIntact() =
        runBlocking {
            store.putString(key, "committed-value")
            storeFile.parentFile?.setWritable(false)

            var caughtWriteFailure: IOException? = null
            try {
                store.putString(key, "value-that-must-not-persist")
            } catch (e: IOException) {
                caughtWriteFailure = e
            } finally {
                storeFile.parentFile?.setWritable(true)
            }

            assertNotNull(caughtWriteFailure)
            assertEquals("committed-value", store.getString(key))
        }

    private companion object {
        const val STORE_FILE_NAME = "core_secure_store"
    }
}
