package com.thanhng224.androidxmlbase.core.startup

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class ThemeApplyInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, AppStartupEntryPoint::class.java)
        val themeManager = entryPoint.themeManager()
        themeManager.currentTheme
            .onEach { themeManager.applyTheme(it) }
            .flowOn(Dispatchers.Main)
            .launchIn(entryPoint.applicationScope())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(TimberInitializer::class.java)
}
