package com.example.androidcorebase

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.androidcorebase.logging.AppReleaseTree
import com.example.androidcorebase.startup.AppStartupCoordinator
import com.thanhng224.androidcorebase.core.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Application entry point: plants logging, launches bounded theme startup, and configures WorkManager. */
@HiltAndroidApp
class AndroidCoreBaseApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var startupCoordinator: AppStartupCoordinator

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        Timber.plant(if (isDebuggable) Timber.DebugTree() else AppReleaseTree())
        applicationScope.launch { startupCoordinator.initialize() }
    }
}
