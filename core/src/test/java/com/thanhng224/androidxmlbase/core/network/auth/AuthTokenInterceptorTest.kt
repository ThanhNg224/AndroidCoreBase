package com.thanhng224.androidxmlbase.core.network.auth

import com.thanhng224.androidxmlbase.core.network.auth.AuthTokenProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class FakeAuthTokenProvider(
    private val token: String?,
) : AuthTokenProvider {
    override suspend fun getToken(): String? = token
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

    private fun clientWith(token: String?): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(AuthTokenInterceptor(FakeAuthTokenProvider(token)))
            .build()

    @Test
    fun `adds raw authorization header when a token is available`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWith(token = "known-token")

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("known-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun `does not add authorization header when token is null`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWith(token = null)

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `does not add authorization header when token is blank`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWith(token = "")

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }
}
