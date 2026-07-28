package com.example.androidxmlbase.feature.settings.domain.usecase

import com.example.androidxmlbase.feature.settings.domain.repository.SettingsRepository
import com.thanhng224.androidxmlbase.core.architecture.UseCase
import com.thanhng224.androidxmlbase.core.ui.theme.AppTheme
import javax.inject.Inject

class SetThemeUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : UseCase<AppTheme, Unit> {
        override suspend fun invoke(params: AppTheme) {
            repository.setTheme(params)
        }
    }
