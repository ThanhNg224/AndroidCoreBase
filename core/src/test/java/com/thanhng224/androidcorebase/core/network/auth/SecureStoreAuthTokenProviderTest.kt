package com.thanhng224.androidcorebase.core.network.auth

import com.thanhng224.androidcorebase.core.foundation.SecureStoreKeys
import com.thanhng224.androidcorebase.core.testing.FakeSecureStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureStoreAuthTokenProviderTest {
    @Test
    fun `returns auth token from secure store`() =
        runTest {
            val store = FakeSecureStore()
            store.putString(SecureStoreKeys.AUTH_TOKEN, "secret-token")

            val provider = SecureStoreAuthTokenProvider(AuthSession(store))

            assertEquals("secret-token", provider.getToken())
        }

    @Test
    fun `returns null when auth token is absent`() =
        runTest {
            val provider = SecureStoreAuthTokenProvider(AuthSession(FakeSecureStore()))

            assertNull(provider.getToken())
        }

    @Test
    fun `peekToken is null until getToken has loaded the session`() =
        runTest {
            val store = FakeSecureStore()
            store.putString(SecureStoreKeys.AUTH_TOKEN, "secret-token")
            val provider = SecureStoreAuthTokenProvider(AuthSession(store))

            assertNull(provider.peekToken())

            provider.getToken()

            assertEquals("secret-token", provider.peekToken())
        }
}
