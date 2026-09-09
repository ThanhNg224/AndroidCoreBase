package com.thanhng224.androidcorebase.core.network

import okhttp3.Interceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkClientFactoryTest {
    @Test
    fun `creates client with thirty second connect read and write timeouts`() {
        val client = NetworkClientFactory.createOkHttpClient(config = ApiConfig(baseUrl = "https://example.com/"))

        assertEquals(30_000, client.connectTimeoutMillis)
        assertEquals(30_000, client.readTimeoutMillis)
        assertEquals(30_000, client.writeTimeoutMillis)
    }

    @Test
    fun `applies per-timeout overrides from the supplied config`() {
        val client =
            NetworkClientFactory.createOkHttpClient(
                config =
                    ApiConfig(
                        baseUrl = "https://example.com/",
                        connectTimeoutSeconds = 5,
                        readTimeoutSeconds = 15,
                        writeTimeoutSeconds = 45,
                    ),
            )

        assertEquals(5_000, client.connectTimeoutMillis)
        assertEquals(15_000, client.readTimeoutMillis)
        assertEquals(45_000, client.writeTimeoutMillis)
    }

    @Test
    fun `retrofit base url comes from the supplied config`() {
        val config = ApiConfig(baseUrl = "https://api.example.com/")
        val client = NetworkClientFactory.createOkHttpClient(config = config)

        val retrofit = NetworkClientFactory.createRetrofit(config = config, okHttpClient = client)

        assertEquals("https://api.example.com/", retrofit.baseUrl().toString())
    }

    @Test
    fun `installs no interceptor when the caller supplies none`() {
        val client = NetworkClientFactory.createOkHttpClient(config = ApiConfig(baseUrl = "https://example.com/"))

        assertTrue(client.interceptors.isEmpty())
    }

    @Test
    fun `installs exactly the caller-supplied interceptors, in order`() {
        val first = Interceptor { chain -> chain.proceed(chain.request()) }
        val second = Interceptor { chain -> chain.proceed(chain.request()) }

        val client =
            NetworkClientFactory.createOkHttpClient(
                config = ApiConfig(baseUrl = "https://example.com/"),
                interceptors = listOf(first, second),
            )

        assertEquals(listOf(first, second), client.interceptors)
    }
}
