package com.thanhng224.androidxmlbase.core.di

import android.content.Context
import com.thanhng224.androidxmlbase.core.BuildConfig
import com.thanhng224.androidxmlbase.core.network.ApiClient
import com.thanhng224.androidxmlbase.core.network.ApiConfig
import com.thanhng224.androidxmlbase.core.network.NetworkClientFactory
import com.thanhng224.androidxmlbase.core.network.RetrofitApiClient
import com.thanhng224.androidxmlbase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidxmlbase.core.network.auth.TokenAuthenticator
import com.thanhng224.androidxmlbase.core.network.connectivity.AndroidConnectivityChecker
import com.thanhng224.androidxmlbase.core.network.connectivity.ConnectivityChecker
import com.thanhng224.androidxmlbase.core.network.transfer.FileTransferClient
import com.thanhng224.androidxmlbase.core.network.transfer.OkHttpFileTransferClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
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
}

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig =
        ApiConfig(
            baseUrl = BuildConfig.API_BASE_URL,
            enableLogging = BuildConfig.API_ENABLE_LOGGING,
        )

    @Provides
    @Singleton
    fun provideConnectivityChecker(
        @ApplicationContext context: Context,
    ): ConnectivityChecker = AndroidConnectivityChecker(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        config: ApiConfig,
        authTokenProvider: AuthTokenProvider,
        connectivityChecker: ConnectivityChecker,
        authenticator: TokenAuthenticator,
    ): OkHttpClient =
        NetworkClientFactory.createOkHttpClient(
            config = config,
            authTokenProvider = authTokenProvider,
            connectivityChecker = connectivityChecker,
            authenticator = authenticator,
        )

    @Provides
    @Singleton
    fun provideRetrofit(
        config: ApiConfig,
        okHttpClient: OkHttpClient,
    ): Retrofit =
        NetworkClientFactory.createRetrofit(
            config = config,
            okHttpClient = okHttpClient,
        )
}
