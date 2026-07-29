package com.example.androidcorebase.feature.settings.domain.repository

import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeTheme(): Flow<AppTheme>

    fun getCurrentLanguage(): AppLanguage?

    fun getSupportedLanguages(): List<AppLanguage>

    suspend fun setLanguage(language: AppLanguage?)

    suspend fun setTheme(theme: AppTheme)
}
