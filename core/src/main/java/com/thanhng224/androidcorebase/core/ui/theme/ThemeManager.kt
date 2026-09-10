package com.thanhng224.androidcorebase.core.ui.theme

import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatDelegate
import com.thanhng224.androidcorebase.core.foundation.SettingsStore
import com.thanhng224.androidcorebase.core.storage.settings.AppSettingsKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

public interface ThemeManager {
    public val currentTheme: Flow<AppTheme>

    /** True once the persisted theme has been read and applied at least once this process. */
    public val isThemeApplied: StateFlow<Boolean>

    public suspend fun getTheme(): AppTheme

    /**
     * Persists [theme] and applies it. Applying recreates live Activities, so call this from a
     * coroutine on the main dispatcher -- `viewModelScope` already is one.
     */
    @MainThread
    public suspend fun setTheme(theme: AppTheme)

    /**
     * Applies [theme] without persisting it.
     *
     * Delegates to [AppCompatDelegate.setDefaultNightMode], which calls `Activity.recreate()` on
     * every live Activity and therefore throws `IllegalStateException: Must be called from main
     * thread` off the main thread. It only throws once an Activity delegate exists, so calling it
     * from a background thread during startup is a race that passes on fast devices and crashes on
     * slow ones.
     */
    @MainThread
    public fun applyTheme(theme: AppTheme)

    public companion object {
        public fun create(settingsStore: SettingsStore): ThemeManager = AndroidThemeManager(settingsStore)
    }
}

internal class AndroidThemeManager
    internal constructor(
        private val settingsStore: SettingsStore,
    ) : ThemeManager {
        private val themeAppliedState = MutableStateFlow(false)
        override val isThemeApplied: StateFlow<Boolean> = themeAppliedState.asStateFlow()

        override val currentTheme: Flow<AppTheme> =
            settingsStore
                .observe(AppSettingsKeys.THEME_MODE)
                .map { AppTheme.fromKey(it) }

        override suspend fun getTheme(): AppTheme {
            val key = settingsStore.get(AppSettingsKeys.THEME_MODE)
            return AppTheme.fromKey(key)
        }

        override suspend fun setTheme(theme: AppTheme) {
            settingsStore.set(AppSettingsKeys.THEME_MODE, theme.key)
            applyTheme(theme)
        }

        override fun applyTheme(theme: AppTheme) {
            val nightMode =
                when (theme) {
                    AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            AppCompatDelegate.setDefaultNightMode(nightMode)
            themeAppliedState.value = true
        }
    }
