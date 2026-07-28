package com.thanhng224.androidxmlbase.core.ui.transition

import android.os.Bundle

/**
 * A single unit of async work run by [com.thanhng224.androidxmlbase.core.ui.base.TransitionActivity]
 * while it covers the screen. Register implementations via a Hilt `@IntoMap` binding keyed by a
 * unique action key so callers can request them by that key without a new Activity subclass.
 */
fun interface TransitionAction {
    suspend fun perform(extras: Bundle)
}
