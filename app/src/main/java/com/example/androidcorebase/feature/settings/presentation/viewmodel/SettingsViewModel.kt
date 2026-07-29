package com.example.androidcorebase.feature.settings.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.androidcorebase.feature.settings.domain.usecase.GetCurrentLanguageUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.GetSupportedLanguagesUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.ObserveThemeUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.SetThemeUseCase
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiEffect
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiEvent
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiState
import com.thanhng224.androidcorebase.core.architecture.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        observeTheme: ObserveThemeUseCase,
        getCurrentLanguage: GetCurrentLanguageUseCase,
        getSupportedLanguages: GetSupportedLanguagesUseCase,
        private val setTheme: SetThemeUseCase,
    ) : StateViewModel<SettingsUiState, SettingsUiEvent, SettingsUiEffect>(
            SettingsUiState(
                language = getCurrentLanguage(),
                supportedLanguages = getSupportedLanguages(),
            ),
        ) {
        init {
            viewModelScope.launch {
                observeTheme().collect { theme -> setState { copy(theme = theme) } }
            }
        }

        override fun onEvent(event: SettingsUiEvent) {
            when (event) {
                is SettingsUiEvent.ThemeSelected -> selectTheme(event)
                is SettingsUiEvent.LanguageSelected -> selectLanguage(event)
            }
        }

        private fun selectTheme(event: SettingsUiEvent.ThemeSelected) {
            if (event.theme == currentState.theme) return
            viewModelScope.launch { setTheme(event.theme) }
        }

        private fun selectLanguage(event: SettingsUiEvent.LanguageSelected) {
            if (event.language == currentState.language) return
            setState { copy(language = event.language) }
            sendEffect(SettingsUiEffect.ApplyLanguage(event.language))
        }
    }
