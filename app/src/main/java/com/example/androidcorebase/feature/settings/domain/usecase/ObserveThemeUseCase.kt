package com.example.androidcorebase.feature.settings.domain.usecase

import com.example.androidcorebase.feature.settings.domain.repository.SettingsRepository
import com.thanhng224.androidcorebase.core.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) {
        operator fun invoke(): Flow<AppTheme> = repository.observeTheme()
    }
