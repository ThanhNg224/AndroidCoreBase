package com.thanhng224.androidcorebase.core.network.auth

import com.thanhng224.androidcorebase.core.storage.secure.SecureStoreKeys
import com.thanhng224.androidcorebase.core.testing.FakeSecureStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthSessionTest {
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
