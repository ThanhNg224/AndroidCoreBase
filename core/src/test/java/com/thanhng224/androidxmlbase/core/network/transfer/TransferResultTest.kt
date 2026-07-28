package com.thanhng224.androidxmlbase.core.network.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferResultTest {
    @Test
    fun `progress exposes integer percent when total is known`() {
        val progress = TransferResult.Progress(bytesTransferred = 25, totalBytes = 100)

        assertEquals(25, progress.percent)
    }

    @Test
    fun `progress percent is null when total is unknown`() {
        val progress = TransferResult.Progress(bytesTransferred = 25, totalBytes = -1)

        assertNull(progress.percent)
    }
}
