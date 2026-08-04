package com.thanhng224.androidcorebase.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenInterceptor
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidcorebase.core.network.connectivity.ConnectivityChecker
import com.thanhng224.androidcorebase.core.network.connectivity.ConnectivityInterceptor
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

internal object NetworkClientFactory {
    fun createOkHttpClient(
        config: ApiConfig,
        authTokenProvider: AuthTokenProvider,
        connectivityChecker: ConnectivityChecker,
        authenticator: Authenticator = Authenticator.NONE,
    ): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level =
                    if (config.enableLogging) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                redactHeader("Authorization")
            }
        return OkHttpClient
            .Builder()
            .addInterceptor(ConnectivityInterceptor(connectivityChecker))
            .addInterceptor(AuthTokenInterceptor(authTokenProvider))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            .authenticator(authenticator)
            .build()
    }

    fun createRetrofit(
        config: ApiConfig,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit
            .Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
