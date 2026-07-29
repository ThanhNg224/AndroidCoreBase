package com.thanhng224.androidcorebase.core.network.auth

import com.thanhng224.androidcorebase.core.network.auth.AuthTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

internal class AuthTokenInterceptor(
    private val authTokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authTokenProvider.getToken()?.takeIf(String::isNotBlank) }
        val request =
            chain.request().let { original ->
                if (token != null) {
                    original.newBuilder().addHeader("Authorization", token).build()
                } else {
                    original
                }
            }
        return chain.proceed(request)
    }
}
