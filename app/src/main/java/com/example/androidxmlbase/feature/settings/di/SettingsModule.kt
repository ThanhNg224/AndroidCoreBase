package com.example.androidxmlbase.feature.settings.di

import com.example.androidxmlbase.feature.settings.data.repository.SettingsRepositoryImpl
import com.example.androidxmlbase.feature.settings.domain.repository.SettingsRepository
import com.example.androidxmlbase.feature.settings.presentation.ui.LanguageTransitionAction
import com.thanhng224.androidxmlbase.core.ui.transition.TransitionAction
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @IntoMap
    @StringKey(LanguageTransitionAction.KEY)
    abstract fun bindLanguageTransitionAction(implementation: LanguageTransitionAction): TransitionAction
}
