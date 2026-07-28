package com.thanhng224.androidxmlbase.core.logging

import android.util.Log
import timber.log.Timber

/**
 * Release-build Timber tree: drops VERBOSE/DEBUG/INFO entirely and forwards only WARN+ to
 * `android.util.Log`. Hook a crash-reporting SDK's `log()`/`recordException()` into [log] once
 * this base picks one — kept as plain `Log.println` until then so no reporting vendor is
 * hardcoded into the base.
 */
class ReleaseTree : Timber.Tree() {
    public override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean = priority >= Log.WARN

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (isLoggable(tag, priority)) {
            Log.println(priority, tag, message)
        }
    }
}
