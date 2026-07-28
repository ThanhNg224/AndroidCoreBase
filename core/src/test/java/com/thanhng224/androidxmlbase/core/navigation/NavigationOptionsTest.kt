package com.thanhng224.androidxmlbase.core.navigation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationOptionsTest {
    @Test
    fun `default options produce no flags`() {
        assertEquals(0, NavigationOptions().toIntentFlags())
    }

    @Test
    fun `clear task includes new task and clear task flags`() {
        val flags = NavigationOptions(clearTask = true).toIntentFlags()

        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, flags)
    }

    @Test
    fun `all options combine flags`() {
        val flags =
            NavigationOptions(
                clearTask = true,
                singleTop = true,
                noAnimation = true,
            ).toIntentFlags()

        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_NO_ANIMATION,
            flags,
        )
    }
}
