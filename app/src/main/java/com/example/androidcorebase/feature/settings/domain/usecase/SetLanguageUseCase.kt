package com.example.androidcorebase.feature.settings.domain.usecase

import com.example.androidcorebase.feature.settings.domain.repository.SettingsRepository
import com.thanhng224.androidcorebase.core.architecture.UseCase
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import javax.inject.Inject

class SetLanguageUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : UseCase<AppLanguage?, Unit> {
        override suspend fun invoke(params: AppLanguage?) {
            repository.setLanguage(params)
        }
    }
