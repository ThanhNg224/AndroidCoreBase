package com.thanhng224.androidcorebase.core.network.auth

import com.thanhng224.androidcorebase.core.foundation.SecureStore
import com.thanhng224.androidcorebase.core.foundation.SecureStoreKey
import com.thanhng224.androidcorebase.core.foundation.SecureStoreKeys
import com.thanhng224.androidcorebase.core.testing.FakeSecureStore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthSessionTest {
    private class CountingSecureStore(
        private val delegate: SecureStore,
    ) : SecureStore {
        private val getStringCallCounts = mutableMapOf<SecureStoreKey, Int>()

        fun getStringCallCount(key: SecureStoreKey): Int = getStringCallCounts[key] ?: 0

        override suspend fun getString(key: SecureStoreKey): String? {
            getStringCallCounts[key] = getStringCallCount(key) + 1
            return delegate.getString(key)
        }

        override suspend fun putString(
            key: SecureStoreKey,
            value: String,
        ) = delegate.putString(key, value)

        override suspend fun remove(key: SecureStoreKey) = delegate.remove(key)

        override suspend fun clear() = delegate.clear()
    }

    @Test
    fun `setTokens persists both tokens when a refresh token is supplied`() =
        runTest {
            val store = FakeSecureStore()
            val session = AuthSession(store)

            session.setTokens(accessToken = "access", refreshToken = "refresh")

            assertEquals("access", session.getAccessToken())
            assertEquals("refresh", session.getRefreshToken())
        }

    @Test
    fun `setTokens leaves an existing refresh token untouched when none is supplied`() =
        runTest {
            val store = FakeSecureStore()
            store.putString(SecureStoreKeys.REFRESH_TOKEN, "original-refresh")
            val session = AuthSession(store)

            session.setTokens(accessToken = "rotated-access")

            assertEquals("rotated-access", session.getAccessToken())
            assertEquals("original-refresh", session.getRefreshToken())
        }

    @Test
    fun `clear removes both tokens`() =
        runTest {
            val store = FakeSecureStore()
            val session = AuthSession(store)
            session.setTokens(accessToken = "access", refreshToken = "refresh")

            session.clear()

            assertNull(session.getAccessToken())
            assertNull(session.getRefreshToken())
        }

    @Test
    fun `concurrent getAccessToken calls load the secure store only once`() =
        runTest {
            val store = FakeSecureStore(mapOf(SecureStoreKeys.AUTH_TOKEN.name to "cached-token"))
            val countingStore = CountingSecureStore(store)
            val session = AuthSession(countingStore)

            coroutineScope {
                repeat(100) { launch { session.getAccessToken() } }
            }

            assertEquals(1, countingStore.getStringCallCount(SecureStoreKeys.AUTH_TOKEN))
        }

    @Test
    fun `getAccessToken reads the in-memory snapshot after setTokens without touching storage again`() =
        runTest {
            val countingStore = CountingSecureStore(FakeSecureStore())
            val session = AuthSession(countingStore)

            session.setTokens(accessToken = "fresh-token")
            val callCountAfterSet = countingStore.getStringCallCount(SecureStoreKeys.AUTH_TOKEN)
            val result = session.getAccessToken()

            assertEquals("fresh-token", result)
            assertEquals(callCountAfterSet, countingStore.getStringCallCount(SecureStoreKeys.AUTH_TOKEN))
        }

    @Test
    fun `peekAccessToken returns null before the first load and the cached value after`() =
        runTest {
            val store = FakeSecureStore(mapOf(SecureStoreKeys.AUTH_TOKEN.name to "cached-token"))
            val session = AuthSession(store)

            assertNull(session.peekAccessToken())

            session.getAccessToken()

            assertEquals("cached-token", session.peekAccessToken())
        }
}
