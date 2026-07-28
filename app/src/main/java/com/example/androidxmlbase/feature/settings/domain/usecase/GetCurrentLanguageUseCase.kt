package com.example.androidxmlbase.feature.settings.domain.usecase

import com.example.androidxmlbase.feature.settings.domain.repository.SettingsRepository
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import javax.inject.Inject

class GetCurrentLanguageUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) {
        operator fun invoke(): AppLanguage? = repository.getCurrentLanguage()
    }
