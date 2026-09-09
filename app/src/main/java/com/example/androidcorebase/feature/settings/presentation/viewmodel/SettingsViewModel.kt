package com.example.androidcorebase.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidcorebase.feature.settings.domain.usecase.GetCurrentLanguageUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.GetSupportedLanguagesUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.ObserveThemeUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.SetThemeUseCase
import com.example.androidcorebase.feature.settings.presentation.state.PendingLanguageTransition
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiEvent
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    ) : ViewModel() {
        private val mutableState =
            MutableStateFlow(
                SettingsUiState(
                    language = getCurrentLanguage(),
                    supportedLanguages = getSupportedLanguages(),
                ),
            )
        val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                observeTheme().collect { theme -> mutableState.update { it.copy(theme = theme) } }
            }
        }

        fun onEvent(event: SettingsUiEvent) {
            when (event) {
                is SettingsUiEvent.ThemeSelected -> selectTheme(event)
                is SettingsUiEvent.LanguageSelected -> selectLanguage(event)
            }
        }

        fun onLanguageTransitionHandled() {
            mutableState.update { it.copy(pendingLanguageTransition = null) }
        }

        private fun selectTheme(event: SettingsUiEvent.ThemeSelected) {
            if (event.theme == mutableState.value.theme) return
            viewModelScope.launch { setTheme(event.theme) }
        }

        private fun selectLanguage(event: SettingsUiEvent.LanguageSelected) {
            if (event.language == mutableState.value.language) return
            mutableState.update {
                it.copy(
                    language = event.language,
                    pendingLanguageTransition = PendingLanguageTransition(event.language),
                )
            }
        }
    }
