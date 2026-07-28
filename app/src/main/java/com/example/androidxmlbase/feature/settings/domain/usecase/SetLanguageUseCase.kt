package com.example.androidxmlbase.feature.settings.domain.usecase

import com.example.androidxmlbase.feature.settings.domain.repository.SettingsRepository
import com.thanhng224.androidxmlbase.core.architecture.UseCase
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
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
