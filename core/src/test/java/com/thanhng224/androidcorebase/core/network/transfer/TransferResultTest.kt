package com.thanhng224.androidcorebase.core.network.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.io.IOException

class TransferResultTest {
    @Test
    fun `Progress with equal fields compares equal`() {
        assertEquals(
            TransferEvent.Progress(bytesTransferred = 10, totalBytes = 100),
            TransferEvent.Progress(bytesTransferred = 10, totalBytes = 100),
        )
    }

    @Test
    fun `Progress represents an unknown total as null, never a negative sentinel`() {
        val progress = TransferEvent.Progress(bytesTransferred = 10, totalBytes = null)

        assertNull(progress.totalBytes)
    }

    @Test
    fun `DownloadEvent Completed and Failed are distinct`() {
        val completed: DownloadEvent = TransferEvent.Completed(File("x"))
        val failed: DownloadEvent = TransferEvent.Failed(TransferError.EmptyBody)

        assertNotEquals(completed, failed)
    }

    @Test
    fun `UploadEvent Completed carries response status metadata, not a body`() {
        val completed: TransferEvent.Completed<HttpTransferMetadata> =
            TransferEvent.Completed(HttpTransferMetadata(code = 200, headers = mapOf("X" to listOf("Y"))))

        assertEquals(200, completed.value.code)
        assertEquals(listOf("Y"), completed.value.headers["X"])
    }

    @Test
    fun `StreamEvent Payload carries the raw chunk bytes`() {
        val payload = TransferEvent.Payload<ByteArray>("chunk".toByteArray())

        assertEquals("chunk", payload.value.toString(Charsets.UTF_8))
    }

    @Test
    fun `TransferError variants each carry their own cause`() {
        val networkCause = IOException("disconnected")
        val fsCause = IOException("disk full")

        assertEquals(networkCause, TransferError.Network(networkCause).cause)
        assertEquals(fsCause, TransferError.FileSystem(fsCause).cause)
    }
}
