package com.thanhng224.androidcorebase.core.network.auth

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** A provider that is already warm: [peekToken] returns [token] directly, [getToken] is never needed. */
private class FakeAuthTokenProvider(
    private val token: String?,
) : AuthTokenProvider {
    var getTokenCallCount = 0
        private set

    override fun peekToken(): String? = token

    override suspend fun getToken(): String? {
        getTokenCallCount++
        return token
    }
}

/** A provider that is always cold: [peekToken] returns null, forcing the interceptor's [getToken] bridge. */
private class ColdAuthTokenProvider(
    private val tokenOnLoad: String?,
) : AuthTokenProvider {
    var getTokenCallCount = 0
        private set

    override fun peekToken(): String? = null

    override suspend fun getToken(): String? {
        getTokenCallCount++
        return tokenOnLoad
    }
}

class AuthTokenInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun clientWith(provider: AuthTokenProvider): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(AuthTokenInterceptor(provider))
            .build()

    @Test
    fun `adds raw authorization header when a token is available`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWith(FakeAuthTokenProvider(token = "known-token"))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("known-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun `does not add authorization header when token is null`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWith(FakeAuthTokenProvider(token = null))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `does not add authorization header when token is blank`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWith(FakeAuthTokenProvider(token = ""))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `warm snapshot never calls the suspending getToken`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val provider = FakeAuthTokenProvider(token = "known-token")
        val client = clientWith(provider)

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        assertEquals(0, provider.getTokenCallCount)
    }

    @Test
    fun `cold path loads the token via suspending getToken on the very first request`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val provider = ColdAuthTokenProvider(tokenOnLoad = "loaded-token")
        val client = clientWith(provider)

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("loaded-token", recorded.getHeader("Authorization"))
        assertEquals(1, provider.getTokenCallCount)
    }
}
