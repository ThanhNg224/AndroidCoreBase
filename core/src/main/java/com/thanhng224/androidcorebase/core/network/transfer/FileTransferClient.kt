package com.thanhng224.androidcorebase.core.network.transfer

import kotlinx.coroutines.flow.Flow
import okhttp3.Request
import java.io.File

public interface FileTransferClient {
    public fun download(
        request: Request,
        destination: File,
    ): Flow<DownloadEvent>

    public fun upload(request: Request): Flow<UploadEvent>

    public fun stream(
        request: Request,
        chunkSizeBytes: Int = DEFAULT_STREAM_CHUNK_SIZE_BYTES,
    ): Flow<StreamEvent>

    public companion object {
        public const val DEFAULT_STREAM_CHUNK_SIZE_BYTES: Int = 8 * 1024
    }
}
