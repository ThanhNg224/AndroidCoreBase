package com.example.androidcorebase.feature.settings.presentation.state

import com.thanhng224.androidcorebase.core.architecture.UiState
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage? = null,
    val supportedLanguages: List<AppLanguage> = emptyList(),
) : UiState
