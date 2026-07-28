package com.example.androidxmlbase.feature.settings.presentation.viewmodel

import app.cash.turbine.test
import com.example.androidxmlbase.feature.settings.domain.repository.SettingsRepository
import com.example.androidxmlbase.feature.settings.domain.usecase.GetCurrentLanguageUseCase
import com.example.androidxmlbase.feature.settings.domain.usecase.ObserveThemeUseCase
import com.example.androidxmlbase.feature.settings.domain.usecase.SetThemeUseCase
import com.example.androidxmlbase.feature.settings.presentation.state.SettingsUiEffect
import com.example.androidxmlbase.feature.settings.presentation.state.SettingsUiEvent
import com.example.androidxmlbase.testutil.MainDispatcherRule
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import com.thanhng224.androidxmlbase.core.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSettingsRepository(
        language: AppLanguage? = AppLanguage.ENGLISH,
        theme: AppTheme = AppTheme.SYSTEM,
    ) : SettingsRepository {
        private val themeFlow = MutableStateFlow(theme)
        private var currentLanguage = language
        var setThemeCalls = 0
            private set

        override fun observeTheme(): Flow<AppTheme> = themeFlow

        override fun getCurrentLanguage(): AppLanguage? = currentLanguage

        override suspend fun setLanguage(language: AppLanguage?) {
            currentLanguage = language
        }

        override suspend fun setTheme(theme: AppTheme) {
            setThemeCalls += 1
            themeFlow.value = theme
        }
    }

    @Test
    fun `initial state reflects the current language and observed theme`() =
        runTest {
            val viewModel = createViewModel(FakeSettingsRepository(AppLanguage.VIETNAMESE, AppTheme.DARK))

            advanceUntilIdle()

            assertEquals(AppLanguage.VIETNAMESE, viewModel.state.value.language)
            assertEquals(AppTheme.DARK, viewModel.state.value.theme)
        }

    @Test
    fun `theme selection persists and updates the shared screen state`() =
        runTest {
            val repository = FakeSettingsRepository(theme = AppTheme.SYSTEM)
            val viewModel = createViewModel(repository)

            advanceUntilIdle()
            viewModel.onEvent(SettingsUiEvent.ThemeSelected(AppTheme.DARK))
            advanceUntilIdle()

            assertEquals(AppTheme.DARK, viewModel.state.value.theme)
            assertEquals(1, repository.setThemeCalls)
        }

    @Test
    fun `selecting the current theme does not persist again`() =
        runTest {
            val repository = FakeSettingsRepository(theme = AppTheme.LIGHT)
            val viewModel = createViewModel(repository)

            advanceUntilIdle()
            viewModel.onEvent(SettingsUiEvent.ThemeSelected(AppTheme.LIGHT))
            advanceUntilIdle()

            assertEquals(0, repository.setThemeCalls)
        }

    @Test
    fun `language selection updates state and requests the safe locale transition`() =
        runTest {
            val viewModel = createViewModel(FakeSettingsRepository(language = AppLanguage.ENGLISH))

            viewModel.effect.test {
                viewModel.onEvent(SettingsUiEvent.LanguageSelected(AppLanguage.VIETNAMESE))

                assertEquals(AppLanguage.VIETNAMESE, viewModel.state.value.language)
                assertEquals(SettingsUiEffect.ApplyLanguage(AppLanguage.VIETNAMESE), awaitItem())
            }
        }

    private fun createViewModel(repository: SettingsRepository): SettingsViewModel =
        SettingsViewModel(
            observeTheme = ObserveThemeUseCase(repository),
            getCurrentLanguage = GetCurrentLanguageUseCase(repository),
            setTheme = SetThemeUseCase(repository),
        )
}
