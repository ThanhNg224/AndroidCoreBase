package com.example.androidcorebase.di

import com.example.androidcorebase.BuildConfig
import com.thanhng224.androidcorebase.core.foundation.AppDispatchers
import com.thanhng224.androidcorebase.core.foundation.SecureStore
import com.thanhng224.androidcorebase.core.network.ApiClient
import com.thanhng224.androidcorebase.core.network.ApiConfig
import com.thanhng224.androidcorebase.core.network.NetworkClientFactory
import com.thanhng224.androidcorebase.core.network.auth.AuthSession
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenInterceptor
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidcorebase.core.network.transfer.FileTransferClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Supplies application-level network configuration and wiring for network clients.
 *
 * This module is deliberately complete rather than minimal: it is the starter's worked example of
 * wiring every `:core` network capability into an app-owned Hilt graph, since `:core` itself ships
 * no DI framework (see `docs/CORE_V2_DESIGN.md`, "Dependency Injection Contract").
 *
 * Only [ApiClient] currently has a consumer ([com.example.androidcorebase.sample.demo.data.datasource.DemoRemoteDataSource]).
 * [AuthSession], [AuthTokenProvider], [Authenticator] and [FileTransferClient] are example wiring:
 * delete the providers your app does not use when you clone this starter. Unused `@Provides`
 * members are never instantiated, so leaving them costs nothing at runtime.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppNetworkModule {
    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig =
        ApiConfig(
            baseUrl = BuildConfig.API_BASE_URL,
            enableLogging = BuildConfig.API_ENABLE_LOGGING,
        )

    @Provides
    @Singleton
    fun provideAuthSession(secureStore: SecureStore): AuthSession = AuthSession(secureStore)

    @Provides
    @Singleton
    fun provideAuthTokenProvider(authSession: AuthSession): AuthTokenProvider = NetworkClientFactory.createAuthTokenProvider(authSession)

    @Provides
    @Singleton
    fun provideAuthenticator(authSession: AuthSession): Authenticator = NetworkClientFactory.createAuthenticator(authSession)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiConfig: ApiConfig,
        authTokenProvider: AuthTokenProvider,
        authenticator: Authenticator,
    ): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = if (apiConfig.enableLogging) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
            }
        return NetworkClientFactory.createOkHttpClient(
            config = apiConfig,
            interceptors = listOf(AuthTokenInterceptor(authTokenProvider), loggingInterceptor),
            authenticator = authenticator,
        )
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        apiConfig: ApiConfig,
        okHttpClient: OkHttpClient,
    ): Retrofit = NetworkClientFactory.createRetrofit(config = apiConfig, okHttpClient = okHttpClient)

    @Provides
    @Singleton
    fun provideApiClient(): ApiClient = NetworkClientFactory.createApiClient()

    @Provides
    @Singleton
    fun provideFileTransferClient(
        okHttpClient: OkHttpClient,
        dispatchers: AppDispatchers,
    ): FileTransferClient = NetworkClientFactory.createFileTransferClient(okHttpClient, dispatchers)
}
