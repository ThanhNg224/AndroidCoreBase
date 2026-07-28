package com.thanhng224.androidxmlbase.core.network.transfer

import java.io.File

sealed interface TransferResult<out T> {
    data class Progress(
        val bytesTransferred: Long,
        val totalBytes: Long,
    ) : TransferResult<Nothing> {
        val percent: Int?
            get() = totalBytes.takeIf { it > 0L }?.let { ((bytesTransferred * 100) / it).toInt() }
    }

    data class Success<T>(
        val data: T,
    ) : TransferResult<T>

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : TransferResult<Nothing>
}

data class HttpTransferResponse(
    val code: Int,
    val body: String?,
)

data class StreamChunk(
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

typealias DownloadResult = TransferResult<File>
typealias UploadResult = TransferResult<HttpTransferResponse>
typealias StreamResult = TransferResult<StreamChunk>
