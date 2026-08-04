package com.thanhng224.androidcorebase.core.time

import android.os.SystemClock
import javax.inject.Inject

/** Monotonic elapsed time for local security windows; it is not affected by wall-clock changes. */
public fun interface ElapsedRealtimeClock {
    public fun nowMillis(): Long
}

internal class AndroidElapsedRealtimeClock
    @Inject
    internal constructor() : ElapsedRealtimeClock {
        override fun nowMillis(): Long = SystemClock.elapsedRealtime()
    }
