package com.thanhng224.androidxmlbase.core.network

import com.thanhng224.androidxmlbase.core.network.auth.NoOpAuthTokenProvider
import com.thanhng224.androidxmlbase.core.network.connectivity.ConnectivityChecker
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkClientFactoryTest {
    @Test
    fun `creates client with thirty second connect read and write timeouts`() {
        val client =
            NetworkClientFactory.createOkHttpClient(
                config = ApiConfig(baseUrl = "https://example.com/", enableLogging = false),
                authTokenProvider = NoOpAuthTokenProvider(),
                connectivityChecker = ConnectedConnectivityChecker,
            )

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
                authTokenProvider = NoOpAuthTokenProvider(),
                connectivityChecker = ConnectedConnectivityChecker,
            )

        assertEquals(5_000, client.connectTimeoutMillis)
        assertEquals(15_000, client.readTimeoutMillis)
        assertEquals(45_000, client.writeTimeoutMillis)
    }

    @Test
    fun `retrofit base url comes from the supplied config`() {
        val config = ApiConfig(baseUrl = "https://api.example.com/")
        val client =
            NetworkClientFactory.createOkHttpClient(
                config = config,
                authTokenProvider = NoOpAuthTokenProvider(),
                connectivityChecker = ConnectedConnectivityChecker,
            )

        val retrofit = NetworkClientFactory.createRetrofit(config = config, okHttpClient = client)

        assertEquals("https://api.example.com/", retrofit.baseUrl().toString())
    }

    private data object ConnectedConnectivityChecker : ConnectivityChecker {
        override fun isConnected(): Boolean = true
    }
}
