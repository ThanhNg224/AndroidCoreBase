package com.thanhng224.androidcorebase.core.di

import android.content.Context
import com.thanhng224.androidcorebase.core.network.ApiClient
import com.thanhng224.androidcorebase.core.network.ApiConfig
import com.thanhng224.androidcorebase.core.network.NetworkClientFactory
import com.thanhng224.androidcorebase.core.network.RetrofitApiClient
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenRefresher
import com.thanhng224.androidcorebase.core.network.auth.TokenAuthenticator
import com.thanhng224.androidcorebase.core.network.connectivity.AndroidConnectivityChecker
import com.thanhng224.androidcorebase.core.network.connectivity.ConnectivityChecker
import com.thanhng224.androidcorebase.core.network.transfer.FileTransferClient
import com.thanhng224.androidcorebase.core.network.transfer.OkHttpFileTransferClient
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
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
    fun provideConnectivityChecker(
        @ApplicationContext context: Context,
    ): ConnectivityChecker = AndroidConnectivityChecker(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiConfig: Optional<ApiConfig>,
        authTokenProvider: AuthTokenProvider,
        connectivityChecker: ConnectivityChecker,
        authenticator: TokenAuthenticator,
    ): OkHttpClient =
        NetworkClientFactory.createOkHttpClient(
            config = apiConfig.orRequireBinding(),
            authTokenProvider = authTokenProvider,
            connectivityChecker = connectivityChecker,
            authenticator = authenticator,
        )

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
