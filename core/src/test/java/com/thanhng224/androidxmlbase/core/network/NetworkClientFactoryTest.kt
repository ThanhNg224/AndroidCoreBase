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

    private data object ConnectedConnectivityChecker : ConnectivityChecker {
        override fun isConnected(): Boolean = true
    }
}
