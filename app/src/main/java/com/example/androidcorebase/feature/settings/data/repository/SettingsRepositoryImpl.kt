package com.example.androidcorebase.feature.settings.data.repository

import com.example.androidcorebase.feature.settings.domain.repository.SettingsRepository
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.localization.LocaleManager
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
import com.thanhng224.androidcorebase.core.ui.theme.ThemeManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl
    @Inject
    constructor(
        private val themeManager: ThemeManager,
        private val localeManager: LocaleManager,
    ) : SettingsRepository {
        override fun observeTheme(): Flow<AppTheme> = themeManager.currentTheme

        override fun getCurrentLanguage(): AppLanguage? = localeManager.currentLanguage()

        override fun getSupportedLanguages(): List<AppLanguage> = localeManager.supportedLanguages()

        override suspend fun setLanguage(language: AppLanguage?) {
            language?.let(localeManager::setLanguage) ?: localeManager.useSystemLanguage()
        }

        override suspend fun setTheme(theme: AppTheme) {
            themeManager.setTheme(theme)
        }
    }
