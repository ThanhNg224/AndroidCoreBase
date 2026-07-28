package com.thanhng224.androidxmlbase.core.ui.theme

import com.thanhng224.androidxmlbase.core.storage.settings.SettingsKey
import com.thanhng224.androidxmlbase.core.storage.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeManagerTest {
    private class FakeSettingsStore : SettingsStore {
        private val data = mutableMapOf<String, Any>()
        private val stateFlow = MutableStateFlow<Map<String, Any>>(emptyMap())

        @Suppress("UNCHECKED_CAST")
        override fun <T> observe(key: SettingsKey<T>): Flow<T> = stateFlow.map { it[key.name] as? T ?: key.defaultValue }

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> get(key: SettingsKey<T>): T = data[key.name] as? T ?: key.defaultValue

        override suspend fun <T> set(
            key: SettingsKey<T>,
            value: T,
        ) {
            data[key.name] = value as Any
            stateFlow.value = data.toMap()
        }

        override suspend fun <T> remove(key: SettingsKey<T>) {
            data.remove(key.name)
            stateFlow.value = data.toMap()
        }
    }

    @Test
    fun `getTheme returns default system theme when no theme is stored`() =
        runTest {
            val settingsStore = FakeSettingsStore()
            val themeManager = AndroidThemeManager(settingsStore)

            val theme = themeManager.getTheme()

            assertEquals(AppTheme.SYSTEM, theme)
        }

    @Test
    fun `setTheme saves selected theme to SettingsStore`() =
        runTest {
            val settingsStore = FakeSettingsStore()
            val themeManager = AndroidThemeManager(settingsStore)

            themeManager.setTheme(AppTheme.DARK)

            assertEquals(AppTheme.DARK, themeManager.getTheme())
        }

    @Test
    fun `currentTheme flow emits stored theme changes`() =
        runTest {
            val settingsStore = FakeSettingsStore()
            val themeManager = AndroidThemeManager(settingsStore)

            settingsStore.set(com.thanhng224.androidxmlbase.core.storage.settings.AppSettingsKeys.THEME_MODE, AppTheme.LIGHT.key)

            val activeTheme = themeManager.currentTheme.first()

            assertEquals(AppTheme.LIGHT, activeTheme)
        }

    @Test
    fun `isThemeApplied stays false until applyTheme runs`() =
        runTest {
            val settingsStore = FakeSettingsStore()
            val themeManager = AndroidThemeManager(settingsStore)

            assertEquals(false, themeManager.isThemeApplied.value)

            themeManager.applyTheme(AppTheme.DARK)

            assertEquals(true, themeManager.isThemeApplied.value)
        }
}
