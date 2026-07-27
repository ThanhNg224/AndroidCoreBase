package com.thanhng224.androidxmlbase.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.thanhng224.androidxmlbase.core.BuildConfig
import com.thanhng224.androidxmlbase.core.logging.ReleaseTree
import timber.log.Timber

internal class TimberInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
