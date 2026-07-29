package com.thanhng224.androidxmlbase.core.network.transfer

import java.io.File

public sealed interface TransferResult<out T> {
    public data class Progress(
        val bytesTransferred: Long,
        val totalBytes: Long,
    ) : TransferResult<Nothing> {
        val percent: Int?
            get() = totalBytes.takeIf { it > 0L }?.let { ((bytesTransferred * 100) / it).toInt() }
    }

    public data class Success<T>(
        val data: T,
    ) : TransferResult<T>

    public data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : TransferResult<Nothing>
}

public data class HttpTransferResponse(
    val code: Int,
    val body: String?,
)

public data class StreamChunk(
    val bytes: ByteArray,
    val bytesRead: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StreamChunk) return false
        return bytes.contentEquals(other.bytes) && bytesRead == other.bytesRead
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + bytesRead.hashCode()
        return result
    }
}

public typealias DownloadResult = TransferResult<File>
public typealias UploadResult = TransferResult<HttpTransferResponse>
public typealias StreamResult = TransferResult<StreamChunk>
