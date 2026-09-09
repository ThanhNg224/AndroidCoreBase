package com.thanhng224.androidcorebase.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

internal object NetworkClientFactory {
    fun createOkHttpClient(
        config: ApiConfig,
        interceptors: List<Interceptor> = emptyList(),
        authenticator: Authenticator = Authenticator.NONE,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
                .authenticator(authenticator)
        interceptors.forEach(builder::addInterceptor)
        return builder.build()
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
