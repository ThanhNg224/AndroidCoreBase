package com.example.androidcorebase.feature.settings.presentation.state

import com.thanhng224.androidcorebase.core.architecture.UiEvent
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme

sealed interface SettingsUiEvent : UiEvent {
    data class ThemeSelected(
        val theme: AppTheme,
    ) : SettingsUiEvent

    data class LanguageSelected(
        val language: AppLanguage?,
    ) : SettingsUiEvent
}
