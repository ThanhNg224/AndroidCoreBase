package com.thanhng224.androidxmlbase.core.network.connectivity

import com.thanhng224.androidxmlbase.core.network.connectivity.ConnectivityChecker
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

private class FakeConnectivityChecker(
    private val connected: Boolean,
) : ConnectivityChecker {
    override fun isConnected(): Boolean = connected
}

class ConnectivityInterceptorTest {
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

    @Test
    fun `throws NoConnectivityException and never reaches the server when disconnected`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(ConnectivityInterceptor(FakeConnectivityChecker(connected = false)))
                .build()

        assertThrows(NoConnectivityException::class.java) {
            client.newCall(Request.Builder().url(server.url("/")).build()).execute()
        }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `proceeds to the server when connected`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(ConnectivityInterceptor(FakeConnectivityChecker(connected = true)))
                .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        assertEquals(1, server.requestCount)
    }
}
