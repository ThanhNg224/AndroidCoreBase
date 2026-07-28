package com.thanhng224.androidxmlbase.core.logging

import android.util.Log
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseTreeTest {
    private val tree = ReleaseTree()

    @Test
    fun `treats warnings and errors as loggable`() {
        assertTrue(tree.isLoggable(tag = "Test", priority = Log.WARN))
        assertTrue(tree.isLoggable(tag = "Test", priority = Log.ERROR))
    }

    @Test
    fun `filters out verbose debug and info priorities`() {
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.VERBOSE))
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.DEBUG))
        assertFalse(tree.isLoggable(tag = "Test", priority = Log.INFO))
    }
}
