package com.thanhng224.androidxmlbase.core.network.transfer

import kotlinx.coroutines.flow.Flow
import okhttp3.Request
import java.io.File

public interface FileTransferClient {
    public fun download(
        request: Request,
        destination: File,
    ): Flow<DownloadResult>

    public fun upload(request: Request): Flow<UploadResult>

    public fun stream(
        request: Request,
        chunkSizeBytes: Int = DEFAULT_STREAM_CHUNK_SIZE_BYTES,
    ): Flow<StreamResult>

    public companion object {
        public const val DEFAULT_STREAM_CHUNK_SIZE_BYTES: Int = 8 * 1024
    }
}
