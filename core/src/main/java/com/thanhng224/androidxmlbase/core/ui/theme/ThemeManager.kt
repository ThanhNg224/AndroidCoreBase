package com.thanhng224.androidxmlbase.core.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.thanhng224.androidxmlbase.core.storage.settings.AppSettingsKeys
import com.thanhng224.androidxmlbase.core.storage.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

public interface ThemeManager {
    public val currentTheme: Flow<AppTheme>

    /** True once the persisted theme has been read and applied at least once this process. */
    public val isThemeApplied: StateFlow<Boolean>

    public suspend fun getTheme(): AppTheme

    public suspend fun setTheme(theme: AppTheme)

    public fun applyTheme(theme: AppTheme)
}

@Singleton
internal class AndroidThemeManager
    @Inject
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
