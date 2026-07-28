package com.thanhng224.androidxmlbase.core.di

import android.content.Context
import com.thanhng224.androidxmlbase.core.architecture.AppDispatchers
import com.thanhng224.androidxmlbase.core.architecture.DefaultAppDispatchers
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import com.thanhng224.androidxmlbase.core.localization.LocaleManager
import com.thanhng224.androidxmlbase.core.localization.SupportedLanguages
import com.thanhng224.androidxmlbase.core.network.auth.AuthTokenProvider
import com.thanhng224.androidxmlbase.core.network.auth.SecureStoreAuthTokenProvider
import com.thanhng224.androidxmlbase.core.storage.secure.EncryptedSecureStore
import com.thanhng224.androidxmlbase.core.storage.secure.SecureStore
import com.thanhng224.androidxmlbase.core.storage.settings.DataStoreSettingsStore
import com.thanhng224.androidxmlbase.core.storage.settings.SettingsStore
import com.thanhng224.androidxmlbase.core.storage.settings.appSettingsDataStore
import com.thanhng224.androidxmlbase.core.time.AndroidElapsedRealtimeClock
import com.thanhng224.androidxmlbase.core.time.ElapsedRealtimeClock
import com.thanhng224.androidxmlbase.core.ui.text.AndroidStringProvider
import com.thanhng224.androidxmlbase.core.ui.text.StringProvider
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
    abstract fun bindSecureStore(implementation: EncryptedSecureStore): SecureStore

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(implementation: SecureStoreAuthTokenProvider): AuthTokenProvider

    @Binds
    @Singleton
    abstract fun bindElapsedRealtimeClock(implementation: AndroidElapsedRealtimeClock): ElapsedRealtimeClock

    @Binds
    @Singleton
    abstract fun bindStringProvider(implementation: AndroidStringProvider): StringProvider

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
    fun provideLocaleManager(supportedLanguages: Optional<SupportedLanguages>): LocaleManager =
        LocaleManager(
            supportedLanguages = supportedLanguages.map(SupportedLanguages::values).orElse(AppLanguage.BUILT_IN),
        )
}
