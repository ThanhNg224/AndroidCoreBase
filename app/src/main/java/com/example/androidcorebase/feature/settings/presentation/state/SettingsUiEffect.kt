package com.example.androidcorebase.feature.settings.presentation.state

import com.thanhng224.androidcorebase.core.architecture.UiEffect
import com.thanhng224.androidcorebase.core.localization.AppLanguage

sealed interface SettingsUiEffect : UiEffect {
    data class ApplyLanguage(
        val language: AppLanguage?,
    ) : SettingsUiEffect
}
