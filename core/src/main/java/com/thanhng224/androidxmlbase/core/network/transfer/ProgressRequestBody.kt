package com.thanhng224.androidxmlbase.core.network.transfer

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.IOException

internal class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()

    @Throws(IOException::class)
    override fun contentLength(): Long = delegate.contentLength()

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        val countingSink =
            object : ForwardingSink(sink) {
                private var bytesWritten = 0L

                @Throws(IOException::class)
                override fun write(
                    source: Buffer,
                    byteCount: Long,
                ) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount
                    onProgress(bytesWritten, totalBytes)
                }
            }
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}
