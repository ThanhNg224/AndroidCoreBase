package com.example.androidcorebase.feature.settings.domain.usecase

import com.example.androidcorebase.feature.settings.domain.repository.SettingsRepository
import com.thanhng224.androidcorebase.core.architecture.UseCase
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
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
