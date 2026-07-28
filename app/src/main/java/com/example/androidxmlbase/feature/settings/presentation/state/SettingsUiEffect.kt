package com.example.androidxmlbase.feature.settings.presentation.state

import com.thanhng224.androidxmlbase.core.architecture.UiEffect
import com.thanhng224.androidxmlbase.core.localization.AppLanguage

sealed interface SettingsUiEffect : UiEffect {
    data class ApplyLanguage(
        val language: AppLanguage?,
    ) : SettingsUiEffect
}
