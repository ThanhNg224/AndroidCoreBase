package com.thanhng224.androidxmlbase.core.startup

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer
import com.thanhng224.androidxmlbase.core.logging.ReleaseTree
import timber.log.Timber

internal class TimberInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // Reads the consuming app's debuggable flag, not :core's own BuildConfig.DEBUG — the
        // latter is baked to false in the published AAR, which would silence DebugTree for
        // every consumer's debug build.
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        Timber.plant(if (isDebuggable) Timber.DebugTree() else ReleaseTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
