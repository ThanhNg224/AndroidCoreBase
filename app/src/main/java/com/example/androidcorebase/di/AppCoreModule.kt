package com.example.androidcorebase.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.thanhng224.androidcorebase.core.foundation.AppDispatchers
import com.thanhng224.androidcorebase.core.foundation.SecureStore
import com.thanhng224.androidcorebase.core.foundation.SettingsStore
import com.thanhng224.androidcorebase.core.localization.AppCompatLocaleApplier
import com.thanhng224.androidcorebase.core.localization.LocaleManager
import com.thanhng224.androidcorebase.core.storage.secure.SecureStoreFactory
import com.thanhng224.androidcorebase.core.storage.settings.SettingsStoreFactory
import com.thanhng224.androidcorebase.core.ui.theme.ThemeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

/** Provides the core library's framework-independent contracts, now that :core ships no Hilt bindings of its own. */
@Module
@InstallIn(SingletonComponent::class)
object AppCoreModule {
    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = AppDispatchers.default()

    @Provides
    @Singleton
    fun provideSettingsStore(
        @ApplicationContext context: Context,
    ): SettingsStore = SettingsStoreFactory.create(context.appSettingsDataStore)

    @Provides
    @Singleton
    fun provideSecureStore(
        @ApplicationContext context: Context,
        dispatchers: AppDispatchers,
    ): SecureStore = SecureStoreFactory.encrypted(context, dispatchers)

    @Provides
    @Singleton
    fun provideThemeManager(settingsStore: SettingsStore): ThemeManager = ThemeManager.create(settingsStore)

    @Provides
    @Singleton
    fun provideLocaleManager(
        @ApplicationContext context: Context,
    ): LocaleManager = LocaleManager(localeApplier = AppCompatLocaleApplier(context))

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
