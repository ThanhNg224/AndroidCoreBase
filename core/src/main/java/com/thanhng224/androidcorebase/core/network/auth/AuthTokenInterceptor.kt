package com.thanhng224.androidcorebase.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

public class AuthTokenInterceptor(
    private val authTokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // The snapshot covers the common warm case with no blocking bridge at all; getToken()
        // only runs (and blocks this interceptor thread) the first time this process needs the
        // token, per the spec's "loads encrypted storage at most once per process" contract.
        val token =
            (authTokenProvider.peekToken() ?: runBlocking { authTokenProvider.getToken() })
                ?.takeIf(String::isNotBlank)
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
