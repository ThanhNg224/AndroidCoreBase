package com.thanhng224.androidxmlbase.core.ui.base

import android.view.View

/**
 * Pure, JVM-testable rate limiter: [shouldAllow] only returns true once per [intervalMs] window,
 * based on caller-supplied timestamps (no dependency on any clock).
 */
class Debouncer(
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    private var lastAllowedAtMs: Long = 0L

    fun shouldAllow(nowMs: Long): Boolean {
        if (nowMs - lastAllowedAtMs < intervalMs) return false
        lastAllowedAtMs = nowMs
        return true
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 600L
    }
}

/**
 * Android-only glue around [Debouncer]: ignores clicks that arrive within [intervalMs] of the
 * last accepted one. Not unit-tested directly (needs a real [View] click dispatch); the rate
 * limiting itself is covered by [Debouncer]'s own tests.
 */
fun View.setOnDebouncedClickListener(
    intervalMs: Long = Debouncer.DEFAULT_INTERVAL_MS,
    action: (View) -> Unit,
) {
    val debouncer = Debouncer(intervalMs)
    setOnClickListener { view ->
        if (debouncer.shouldAllow(System.currentTimeMillis())) action(view)
    }
}
