package com.example.androidcorebase

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Startup work (logging, DB passphrase warm-up, theme application) runs via the
 * `androidx.startup` `Initializer`s in `core/startup/`, registered in AndroidManifest.xml —
 * not here. See `docs/CORE_MODULES.md` → `core/startup`.
 *
 * `Configuration.Provider` supplies `HiltWorkerFactory` so `@HiltWorker` classes (see
 * `core/work/`) get constructor injection; WorkManager's default initializer is disabled in the
 * manifest so this custom configuration is the one actually used.
 */
@HiltAndroidApp
class AndroidCoreBaseApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
