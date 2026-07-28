package com.thanhng224.androidxmlbase.core.network.transfer

import kotlinx.coroutines.flow.Flow
import okhttp3.Request
import java.io.File

interface FileTransferClient {
    fun download(
        request: Request,
        destination: File,
    ): Flow<DownloadResult>

    fun upload(request: Request): Flow<UploadResult>

    fun stream(
        request: Request,
        chunkSizeBytes: Int = DEFAULT_STREAM_CHUNK_SIZE_BYTES,
    ): Flow<StreamResult>

    companion object {
        const val DEFAULT_STREAM_CHUNK_SIZE_BYTES = 8 * 1024
    }
}
