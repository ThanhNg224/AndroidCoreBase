package com.thanhng224.androidxmlbase.core.architecture

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAppDispatchersTest {
    private val dispatchers: AppDispatchers = DefaultAppDispatchers()

    @Test
    fun `exposes the IO dispatcher`() {
        assertEquals(Dispatchers.IO, dispatchers.io)
    }

    @Test
    fun `exposes the Default dispatcher`() {
        assertEquals(Dispatchers.Default, dispatchers.default)
    }
}
