package com.example.androidcorebase.di

import com.example.androidcorebase.BuildConfig
import com.thanhng224.androidcorebase.core.network.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Supplies application-level [ApiConfig] settings for network clients. */
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
}
