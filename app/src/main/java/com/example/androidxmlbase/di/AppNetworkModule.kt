package com.example.androidxmlbase.di

import com.example.androidxmlbase.BuildConfig
import com.thanhng224.androidxmlbase.core.network.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Supplies the [ApiConfig] that :core deliberately does not ship a default for. Every app
 * consuming :core needs a module like this one.
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
}
