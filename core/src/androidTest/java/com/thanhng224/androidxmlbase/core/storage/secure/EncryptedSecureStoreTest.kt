package com.thanhng224.androidxmlbase.core.storage.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanhng224.androidxmlbase.core.architecture.DefaultAppDispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedSecureStoreTest {
    private lateinit var context: Context
    private lateinit var store: EncryptedSecureStore
    private val key = SecureStoreKey("test_key")

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        rawPreferences().edit().clear().commit()
        store = EncryptedSecureStore(context, DefaultAppDispatchers())
    }

    @After
    fun tearDown() {
        rawPreferences().edit().clear().commit()
    }

    private fun rawPreferences(): SharedPreferences = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    @Test
    fun putString_thenGetString_returnsOriginalPlaintext() =
        runTest {
            store.putString(key, "super-secret-value")

            assertEquals("super-secret-value", store.getString(key))
        }

    @Test
    fun getString_withoutPriorPut_returnsNull() =
        runTest {
            assertNull(store.getString(key))
        }

    @Test
    fun storedValue_isNotPlaintextOnDisk() =
        runTest {
            store.putString(key, "super-secret-value")

            val rawOnDisk = rawPreferences().getString(key.name, null)

            assertNotEquals("super-secret-value", rawOnDisk)
        }

    @Test
    fun remove_clearsStoredValue() =
        runTest {
            store.putString(key, "super-secret-value")

            store.remove(key)

            assertNull(store.getString(key))
        }

    @Test
    fun clear_removesAllStoredValues() =
        runTest {
            val otherKey = SecureStoreKey("other_key")
            store.putString(key, "value-one")
            store.putString(otherKey, "value-two")

            store.clear()

            assertNull(store.getString(key))
            assertNull(store.getString(otherKey))
        }

    private companion object {
        const val PREFS_FILE_NAME = "secure_store"
    }
}
