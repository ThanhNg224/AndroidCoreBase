package com.thanhng224.androidcorebase.core.network.connectivity

import com.thanhng224.androidcorebase.core.network.connectivity.ConnectivityChecker
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

public class NoConnectivityException : IOException("No network connection")

internal class ConnectivityInterceptor(
    private val connectivityChecker: ConnectivityChecker,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!connectivityChecker.isConnected()) throw NoConnectivityException()
        return chain.proceed(chain.request())
    }
}
