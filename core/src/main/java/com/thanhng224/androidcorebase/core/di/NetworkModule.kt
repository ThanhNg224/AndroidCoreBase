package com.thanhng224.androidcorebase.core.di

import com.thanhng224.androidcorebase.core.network.ApiClient
import com.thanhng224.androidcorebase.core.network.ApiConfig
import com.thanhng224.androidcorebase.core.network.NetworkClientFactory
import com.thanhng224.androidcorebase.core.network.RetrofitApiClient
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenInterceptor
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenRefresher
import com.thanhng224.androidcorebase.core.network.auth.TokenAuthenticator
import com.thanhng224.androidcorebase.core.network.transfer.FileTransferClient
import com.thanhng224.androidcorebase.core.network.transfer.OkHttpFileTransferClient
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.Optional
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindingsModule {
    @Binds
    @Singleton
    abstract fun bindApiClient(implementation: RetrofitApiClient): ApiClient

    @Binds
    @Singleton
    abstract fun bindFileTransferClient(implementation: OkHttpFileTransferClient): FileTransferClient

    @BindsOptionalOf
    abstract fun bindAuthTokenRefresher(): AuthTokenRefresher

    @BindsOptionalOf
    abstract fun bindApiConfig(): ApiConfig
}

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiConfig: Optional<ApiConfig>,
        authTokenProvider: AuthTokenProvider,
        authenticator: TokenAuthenticator,
    ): OkHttpClient {
        val config = apiConfig.orRequireBinding()
        // NetworkClientFactory itself never builds a body logger; this app-facing DI module owns
        // that policy decision and passes the interceptor in explicitly.
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = if (config.enableLogging) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
            }
        return NetworkClientFactory.createOkHttpClient(
            config = config,
            interceptors = listOf(AuthTokenInterceptor(authTokenProvider), loggingInterceptor),
            authenticator = authenticator,
        )
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        apiConfig: Optional<ApiConfig>,
        okHttpClient: OkHttpClient,
    ): Retrofit =
        NetworkClientFactory.createRetrofit(
            config = apiConfig.orRequireBinding(),
            okHttpClient = okHttpClient,
        )

    private fun Optional<ApiConfig>.orRequireBinding(): ApiConfig =
        orElseThrow {
            IllegalStateException(
                "No ApiConfig binding found. :core does not ship a base URL — provide one from " +
                    "your app's Hilt module:\n\n" +
                    "@Module\n" +
                    "@InstallIn(SingletonComponent::class)\n" +
                    "object AppNetworkModule {\n" +
                    "    @Provides\n" +
                    "    @Singleton\n" +
                    "    fun provideApiConfig() = ApiConfig(baseUrl = \"https://api.example.com/\")\n" +
                    "}",
            )
        }
}
