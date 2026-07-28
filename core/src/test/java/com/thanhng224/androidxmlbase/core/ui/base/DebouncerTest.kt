package com.thanhng224.androidxmlbase.core.ui.base

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebouncerTest {
    @Test
    fun `first call is always allowed`() {
        val debouncer = Debouncer(intervalMs = 600L)

        assertTrue(debouncer.shouldAllow(nowMs = 1_000L))
    }

    @Test
    fun `call within the interval of the last allowed call is denied`() {
        val debouncer = Debouncer(intervalMs = 600L)
        debouncer.shouldAllow(nowMs = 1_000L)

        assertFalse(debouncer.shouldAllow(nowMs = 1_599L))
    }

    @Test
    fun `call exactly at the interval boundary is allowed`() {
        val debouncer = Debouncer(intervalMs = 600L)
        debouncer.shouldAllow(nowMs = 1_000L)

        assertTrue(debouncer.shouldAllow(nowMs = 1_600L))
    }

    @Test
    fun `call after the interval has passed is allowed`() {
        val debouncer = Debouncer(intervalMs = 600L)
        debouncer.shouldAllow(nowMs = 1_000L)

        assertTrue(debouncer.shouldAllow(nowMs = 2_000L))
    }

    @Test
    fun `denied call does not reset the last allowed timestamp`() {
        val debouncer = Debouncer(intervalMs = 600L)
        debouncer.shouldAllow(nowMs = 1_000L)
        debouncer.shouldAllow(nowMs = 1_200L) // denied, should not move the window forward

        assertFalse(debouncer.shouldAllow(nowMs = 1_599L))
        assertTrue(debouncer.shouldAllow(nowMs = 1_600L))
    }
}
