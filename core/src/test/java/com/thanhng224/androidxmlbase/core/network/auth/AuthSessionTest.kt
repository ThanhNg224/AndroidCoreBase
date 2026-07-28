package com.thanhng224.androidxmlbase.core.network.auth

import com.thanhng224.androidxmlbase.core.storage.secure.SecureStore
import com.thanhng224.androidxmlbase.core.storage.secure.SecureStoreKey
import com.thanhng224.androidxmlbase.core.storage.secure.SecureStoreKeys
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthSessionTest {
    private class FakeSecureStore : SecureStore {
        private val values = mutableMapOf<SecureStoreKey, String>()

        override suspend fun getString(key: SecureStoreKey): String? = values[key]

        override suspend fun putString(
            key: SecureStoreKey,
            value: String,
        ) {
            values[key] = value
        }

        override suspend fun remove(key: SecureStoreKey) {
            values.remove(key)
        }

        override suspend fun clear() {
            values.clear()
        }
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
}
