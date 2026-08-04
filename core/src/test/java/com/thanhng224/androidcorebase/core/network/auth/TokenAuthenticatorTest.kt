package com.thanhng224.androidcorebase.core.network.auth

import com.thanhng224.androidcorebase.core.storage.secure.SecureStore
import com.thanhng224.androidcorebase.core.storage.secure.SecureStoreKeys
import com.thanhng224.androidcorebase.core.testing.FakeSecureStore
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Optional
import javax.inject.Provider

class TokenAuthenticatorTest {
    private class FakeAuthTokenRefresher(
        private val newToken: String?,
    ) : AuthTokenRefresher {
        var callCount = 0

        override suspend fun refresh(refreshToken: String?): String? {
            callCount++
            return newToken
        }
    }

    private fun response(
        authorizationHeader: String? = null,
        priorResponse: Response? = null,
    ): Response {
        val request =
            Request
                .Builder()
                .url("https://example.com/")
                .apply { authorizationHeader?.let { header("Authorization", it) } }
                .build()
        return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .apply { priorResponse?.let { priorResponse(it) } }
            .build()
    }

    private fun authenticator(
        secureStore: SecureStore,
        refresher: AuthTokenRefresher? = null,
    ): TokenAuthenticator {
        val optionalRefresher =
            if (refresher != null) Optional.of(Provider { refresher }) else Optional.empty()
        return TokenAuthenticator(AuthSession(secureStore), optionalRefresher)
    }

    @Test
    fun `authenticate returns cached token when it differs from the token that just failed`() =
        runBlocking {
            val store = FakeSecureStore()
            store.putString(SecureStoreKeys.AUTH_TOKEN, "already-newer-token")
            val refresher = FakeAuthTokenRefresher("should-not-be-used")
            val sut = authenticator(store, refresher)

            val result = sut.authenticate(null, response(authorizationHeader = "stale-token"))

            assertEquals("already-newer-token", result?.header("Authorization"))
            assertEquals(0, refresher.callCount)
        }

    @Test
    fun `authenticate refreshes and persists the new token when cached token matches the failed one`() =
        runBlocking {
            val store = FakeSecureStore()
            store.putString(SecureStoreKeys.AUTH_TOKEN, "expired-token")
            val refresher = FakeAuthTokenRefresher("fresh-token")
            val sut = authenticator(store, refresher)

            val result = sut.authenticate(null, response(authorizationHeader = "expired-token"))

            assertEquals("fresh-token", result?.header("Authorization"))
            assertEquals(1, refresher.callCount)
            assertEquals(
                "fresh-token",
                store.getString(SecureStoreKeys.AUTH_TOKEN),
            )
        }

    @Test
    fun `authenticate returns null when no refresher is bound`() =
        runBlocking {
            val store = FakeSecureStore()
            store.putString(SecureStoreKeys.AUTH_TOKEN, "expired-token")
            val sut = authenticator(store, refresher = null)

            val result = sut.authenticate(null, response(authorizationHeader = "expired-token"))

            assertNull(result)
        }

    @Test
    fun `authenticate returns null when the refresher fails`() =
        runBlocking {
            val store = FakeSecureStore()
            store.putString(SecureStoreKeys.AUTH_TOKEN, "expired-token")
            val refresher = FakeAuthTokenRefresher(newToken = null)
            val sut = authenticator(store, refresher)

            val result = sut.authenticate(null, response(authorizationHeader = "expired-token"))

            assertNull(result)
            assertEquals(1, refresher.callCount)
        }

    @Test
    fun `authenticate gives up after too many retries without touching the refresher`() =
        runBlocking {
            val store = FakeSecureStore()
            val refresher = FakeAuthTokenRefresher("fresh-token")
            val sut = authenticator(store, refresher)

            val first = response(authorizationHeader = "t")
            val second = response(authorizationHeader = "t", priorResponse = first)
            val third = response(authorizationHeader = "t", priorResponse = second)

            val result = sut.authenticate(null, third)

            assertNull(result)
            assertEquals(0, refresher.callCount)
        }
}
