package com.thanhng224.androidcorebase.core.di

import android.content.Context
import com.thanhng224.androidcorebase.core.architecture.DefaultAppDispatchers
import com.thanhng224.androidcorebase.core.foundation.AppDispatchers
import com.thanhng224.androidcorebase.core.foundation.SecureStore
import com.thanhng224.androidcorebase.core.foundation.SettingsStore
import com.thanhng224.androidcorebase.core.localization.AppCompatLocaleApplier
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.localization.LocaleManager
import com.thanhng224.androidcorebase.core.localization.SupportedLanguages
import com.thanhng224.androidcorebase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidcorebase.core.network.auth.SecureStoreAuthTokenProvider
import com.thanhng224.androidcorebase.core.storage.secure.EncryptedFileSecureStore
import com.thanhng224.androidcorebase.core.storage.settings.DataStoreSettingsStore
import com.thanhng224.androidcorebase.core.storage.settings.appSettingsDataStore
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppCoreBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAppDispatchers(implementation: DefaultAppDispatchers): AppDispatchers

    @Binds
    @Singleton
    abstract fun bindSecureStore(implementation: EncryptedFileSecureStore): SecureStore

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(implementation: SecureStoreAuthTokenProvider): AuthTokenProvider

    @BindsOptionalOf
    abstract fun bindSupportedLanguages(): SupportedLanguages
}

@Module
@InstallIn(SingletonComponent::class)
internal object AppCoreModule {
    @Provides
    @Singleton
    fun provideSettingsStore(
        @ApplicationContext context: Context,
    ): SettingsStore = DataStoreSettingsStore(context.appSettingsDataStore)

    @Provides
    @Singleton
    fun provideLocaleManager(
        @ApplicationContext context: Context,
        supportedLanguages: Optional<SupportedLanguages>,
    ): LocaleManager =
        LocaleManager(
            localeApplier = AppCompatLocaleApplier(context),
            supportedLanguages = supportedLanguages.map(SupportedLanguages::values).orElse(AppLanguage.BUILT_IN),
        )
}
