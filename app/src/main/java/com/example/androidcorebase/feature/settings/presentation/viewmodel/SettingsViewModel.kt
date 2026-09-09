package com.example.androidcorebase.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidcorebase.R
import com.example.androidcorebase.feature.settings.domain.usecase.GetCurrentLanguageUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.GetSupportedLanguagesUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.ObserveThemeUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.SetLanguageUseCase
import com.example.androidcorebase.feature.settings.domain.usecase.SetThemeUseCase
import com.example.androidcorebase.feature.settings.presentation.state.PendingSettingsMessage
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiEvent
import com.example.androidcorebase.feature.settings.presentation.state.SettingsUiState
import com.thanhng224.androidcorebase.core.ui.text.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        observeTheme: ObserveThemeUseCase,
        private val getCurrentLanguage: GetCurrentLanguageUseCase,
        getSupportedLanguages: GetSupportedLanguagesUseCase,
        private val setTheme: SetThemeUseCase,
        private val setLanguage: SetLanguageUseCase,
    ) : ViewModel() {
        private var isInitialLanguageLoaded = false
        private val nextMessageId = AtomicLong(0)
        private val mutableState = MutableStateFlow(SettingsUiState(supportedLanguages = getSupportedLanguages()))
        val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                observeTheme().collect { theme -> mutableState.update { it.copy(theme = theme) } }
            }
            viewModelScope.launch {
                val language = getCurrentLanguage()
                mutableState.update { it.copy(language = language) }
                isInitialLanguageLoaded = true
            }
        }

        fun onEvent(event: SettingsUiEvent) {
            when (event) {
                is SettingsUiEvent.ThemeSelected -> selectTheme(event)
                is SettingsUiEvent.LanguageSelected -> selectLanguage(event)
            }
        }

        fun onMessageHandled(id: Long) {
            removeHeadIfMatching(id)
        }

        private fun selectTheme(event: SettingsUiEvent.ThemeSelected) {
            if (event.theme == mutableState.value.theme) return
            viewModelScope.launch { setTheme(event.theme) }
        }

        private fun selectLanguage(event: SettingsUiEvent.LanguageSelected) {
            if (!isInitialLanguageLoaded) return
            if (event.language == mutableState.value.language) return
            viewModelScope.launch {
                try {
                    setLanguage(event.language)
                    mutableState.update { it.copy(language = event.language) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: IOException) {
                    enqueueMessage(UiText.StringResource(R.string.settings_language_update_failed))
                }
            }
        }

        private fun enqueueMessage(text: UiText) {
            val message = PendingSettingsMessage(id = nextMessageId.incrementAndGet(), text = text)
            mutableState.update { it.copy(pendingMessages = it.pendingMessages + message) }
        }

        /** Returns whether [id] matched the current head and was removed. */
        private fun removeHeadIfMatching(id: Long): Boolean {
            var removed = false
            mutableState.update { current ->
                val head = current.pendingMessages.firstOrNull()
                if (head?.id != id) {
                    current
                } else {
                    removed = true
                    current.copy(pendingMessages = current.pendingMessages.drop(1))
                }
            }
            return removed
        }
    }
