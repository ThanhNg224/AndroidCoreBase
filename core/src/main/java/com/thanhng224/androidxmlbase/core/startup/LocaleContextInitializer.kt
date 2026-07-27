package com.thanhng224.androidxmlbase.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.thanhng224.androidxmlbase.core.localization.LocaleAppContext

/**
 * Captures the process-wide Application [Context] into [LocaleAppContext] so
 * `AppCompatLocaleApplier.currentLocaleTags()` can read the current per-app locale without
 * depending on any [androidx.appcompat.app.AppCompatDelegate] being alive.
 */
internal class LocaleContextInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        LocaleAppContext.applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
