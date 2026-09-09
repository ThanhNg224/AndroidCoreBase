package com.example.androidcorebase.feature.settings.presentation.viewmodel

import app.cash.turbine.test
import com.example.androidcorebase.feature.settings.domain.repository.SettingsRepository
import com.example.androidcorebase.feature.settings.domain.usecase.GetCurrentLanguageUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.GetSupportedLanguagesUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.ObserveThemeUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.SetLanguageUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.SetThemeUseCase
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiEvent
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.testing.MainDispatcherRule
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSettingsRepository(
        language: AppLanguage? = AppLanguage.ENGLISH,
        theme: AppTheme = AppTheme.SYSTEM,
        private val calls: MutableList<String>? = null,
        private val failLanguagePersistence: Boolean = false,
    ) : SettingsRepository {
        private val themeFlow = MutableStateFlow(theme)
        private var currentLanguage = language
        var setThemeCalls = 0
            private set

        override fun observeTheme(): Flow<AppTheme> = themeFlow

        override suspend fun getCurrentLanguage(): AppLanguage? = currentLanguage

        override fun getSupportedLanguages(): List<AppLanguage> = AppLanguage.BUILT_IN

        override suspend fun setLanguage(language: AppLanguage?) {
            val tag = language?.languageTag ?: "system"
            calls?.add("persist:$tag")
            if (failLanguagePersistence) throw IOException("persist failed")
            calls?.add("apply:$tag")
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
    fun `selecting a language persists before applying it`() =
        runTest {
            val calls = mutableListOf<String>()
            val viewModel = createViewModel(FakeSettingsRepository(language = AppLanguage.ENGLISH, calls = calls))
            advanceUntilIdle()

            viewModel.onEvent(SettingsUiEvent.LanguageSelected(AppLanguage.VIETNAMESE))
            advanceUntilIdle()

            assertEquals(listOf("persist:vi-VN", "apply:vi-VN"), calls)
            assertEquals(AppLanguage.VIETNAMESE, viewModel.state.value.language)
        }

    @Test
    fun `a failed language persistence keeps the previous language and queues an error message`() =
        runTest {
            val calls = mutableListOf<String>()
            val viewModel =
                createViewModel(
                    FakeSettingsRepository(language = AppLanguage.ENGLISH, calls = calls, failLanguagePersistence = true),
                )
            advanceUntilIdle()

            viewModel.onEvent(SettingsUiEvent.LanguageSelected(AppLanguage.VIETNAMESE))
            advanceUntilIdle()

            assertEquals(listOf("persist:vi-VN"), calls)
            assertEquals(AppLanguage.ENGLISH, viewModel.state.value.language)
            assertEquals(1, viewModel.state.value.pendingMessages.size)
        }

    @Test
    fun `acknowledging the language error message removes it`() =
        runTest {
            val viewModel = createViewModel(FakeSettingsRepository(language = AppLanguage.ENGLISH, failLanguagePersistence = true))
            advanceUntilIdle()
            viewModel.onEvent(SettingsUiEvent.LanguageSelected(AppLanguage.VIETNAMESE))
            advanceUntilIdle()
            val message =
                viewModel.state.value.pendingMessages
                    .first()

            viewModel.onMessageHandled(message.id)

            assertEquals(0, viewModel.state.value.pendingMessages.size)
        }

    @Test
    fun `re-collecting state does not repeat the language mutation`() =
        runTest {
            val calls = mutableListOf<String>()
            val viewModel = createViewModel(FakeSettingsRepository(language = AppLanguage.ENGLISH, calls = calls))
            advanceUntilIdle()
            viewModel.onEvent(SettingsUiEvent.LanguageSelected(AppLanguage.VIETNAMESE))
            advanceUntilIdle()

            viewModel.state.test { awaitItem() }
            viewModel.state.test { awaitItem() }

            assertEquals(listOf("persist:vi-VN", "apply:vi-VN"), calls)
        }

    private fun createViewModel(repository: SettingsRepository): SettingsViewModel =
        SettingsViewModel(
            observeTheme = ObserveThemeUseCase(repository),
            getCurrentLanguage = GetCurrentLanguageUseCase(repository),
            getSupportedLanguages = GetSupportedLanguagesUseCase(repository),
            setTheme = SetThemeUseCase(repository),
            setLanguage = SetLanguageUseCase(repository),
        )
}
