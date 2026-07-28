package com.example.androidxmlbase.feature.settings.presentation.state

import com.thanhng224.androidxmlbase.core.architecture.UiState
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import com.thanhng224.androidxmlbase.core.ui.theme.AppTheme

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage? = null,
) : UiState
