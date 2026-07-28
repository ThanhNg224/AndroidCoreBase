package com.thanhng224.androidxmlbase.core.time

import android.os.SystemClock
import javax.inject.Inject

/** Monotonic elapsed time for local security windows; it is not affected by wall-clock changes. */
fun interface ElapsedRealtimeClock {
    fun nowMillis(): Long
}

internal class AndroidElapsedRealtimeClock
    @Inject
    internal constructor() : ElapsedRealtimeClock {
        override fun nowMillis(): Long = SystemClock.elapsedRealtime()
    }
