package com.thanhng224.androidxmlbase.core.network.auth

import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import javax.inject.Provider

class TokenAuthenticatorTest {
    private class FakeAuthTokenProvider(
        private var token: String?,
    ) : AuthTokenProvider {
        override suspend fun getToken(): String? = token
    }

    @Test
    fun `authenticate returns request with new token when original request had different token`() {
        val provider = FakeAuthTokenProvider("new-token")
        val authenticator = TokenAuthenticator(Provider { provider })

        val originalRequest =
            Request
                .Builder()
                .url("https://example.com/")
                .header("Authorization", "old-token")
                .build()

        val response =
            Response
                .Builder()
                .request(originalRequest)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()

        val authenticatedRequest = authenticator.authenticate(null, response)

        assertEquals("new-token", authenticatedRequest?.header("Authorization"))
    }

    @Test
    fun `authenticate returns null when token provider returns null or blank`() {
        val provider = FakeAuthTokenProvider("")
        val authenticator = TokenAuthenticator(Provider { provider })

        val originalRequest =
            Request
                .Builder()
                .url("https://example.com/")
                .build()

        val response =
            Response
                .Builder()
                .request(originalRequest)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()

        val authenticatedRequest = authenticator.authenticate(null, response)

        assertNull(authenticatedRequest)
    }

    @Test
    fun `authenticate returns null when request already contains the latest token to prevent infinite loop`() {
        val provider = FakeAuthTokenProvider("latest-token")
        val authenticator = TokenAuthenticator(Provider { provider })

        val originalRequest =
            Request
                .Builder()
                .url("https://example.com/")
                .header("Authorization", "latest-token")
                .build()

        val response =
            Response
                .Builder()
                .request(originalRequest)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()

        val authenticatedRequest = authenticator.authenticate(null, response)

        assertNull(authenticatedRequest)
    }
}
