package com.thanhng224.androidcorebase.core.startup

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class DbPassphraseWarmupInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, AppStartupEntryPoint::class.java)
        entryPoint.applicationScope().launch(Dispatchers.IO) {
            entryPoint.dbPassphraseProvider().getOrCreate()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(TimberInitializer::class.java)
}
