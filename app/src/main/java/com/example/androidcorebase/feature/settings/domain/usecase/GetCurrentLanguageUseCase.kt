package com.example.androidcorebase.feature.settings.domain.usecase

import com.example.androidcorebase.feature.settings.domain.repository.SettingsRepository
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import javax.inject.Inject

class GetCurrentLanguageUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) {
        operator fun invoke(): AppLanguage? = repository.getCurrentLanguage()
    }
