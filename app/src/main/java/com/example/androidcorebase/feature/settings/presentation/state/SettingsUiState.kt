package com.example.androidcorebase.feature.settings.presentation.state

import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme

/** Wraps the target language so a pending "switch to System" (`language == null`) is still distinguishable from no pending request. */
data class PendingLanguageTransition(
    val language: AppLanguage?,
)

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage? = null,
    val supportedLanguages: List<AppLanguage> = emptyList(),
    val pendingLanguageTransition: PendingLanguageTransition? = null,
)
