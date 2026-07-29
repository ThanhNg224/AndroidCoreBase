package com.thanhng224.androidcorebase.core.startup

import com.thanhng224.androidcorebase.core.di.ApplicationScope
import com.thanhng224.androidcorebase.core.storage.database.DbPassphraseProvider
import com.thanhng224.androidcorebase.core.ui.theme.ThemeManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * androidx.startup `Initializer`s are instantiated by reflection (no-arg constructor) from a
 * `ContentProvider` that runs before `Application.onCreate()`, so they can't use constructor
 * injection. This `EntryPoint` is how they reach Hilt-provided singletons instead. Safe to call
 * this early: Hilt's component is built lazily on first access, not on `Application.onCreate()`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AppStartupEntryPoint {
    fun dbPassphraseProvider(): DbPassphraseProvider

    fun themeManager(): ThemeManager

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
