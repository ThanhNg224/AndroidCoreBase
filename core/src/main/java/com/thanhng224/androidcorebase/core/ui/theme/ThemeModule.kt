package com.thanhng224.androidcorebase.core.ui.theme

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ThemeModule {
    @Binds
    @Singleton
    abstract fun bindThemeManager(implementation: AndroidThemeManager): ThemeManager
}
