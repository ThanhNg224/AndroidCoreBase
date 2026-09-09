package com.example.androidcorebase.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Reference implementation showing the `@HiltWorker` + `CoroutineWorker` pattern this base wires
 * up (Hilt worker factory, `Configuration.Provider` in `AndroidCoreBaseApplication`). Copy this
 * shape for real background work -- this worker itself is not scheduled anywhere by default.
 */
@HiltWorker
class HeartbeatWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParameters: WorkerParameters,
    ) : CoroutineWorker(context, workerParameters) {
        override suspend fun doWork(): Result {
            Timber.tag(TAG).i("Heartbeat worker executed")
            return Result.success()
        }

        private companion object {
            const val TAG = "HeartbeatWorker"
        }
    }
