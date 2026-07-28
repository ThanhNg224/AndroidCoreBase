package com.example.androidxmlbase.feature.settings.domain.repository

import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import com.thanhng224.androidxmlbase.core.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeTheme(): Flow<AppTheme>

    fun getCurrentLanguage(): AppLanguage?

    suspend fun setLanguage(language: AppLanguage?)

    suspend fun setTheme(theme: AppTheme)
}
