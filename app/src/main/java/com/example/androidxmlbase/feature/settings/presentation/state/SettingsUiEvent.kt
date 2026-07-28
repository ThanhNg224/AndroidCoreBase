package com.example.androidxmlbase.feature.settings.presentation.state

import com.thanhng224.androidxmlbase.core.architecture.UiEvent
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import com.thanhng224.androidxmlbase.core.ui.theme.AppTheme

sealed interface SettingsUiEvent : UiEvent {
    data class ThemeSelected(
        val theme: AppTheme,
    ) : SettingsUiEvent

    data class LanguageSelected(
        val language: AppLanguage?,
    ) : SettingsUiEvent
}
