package com.thanhng224.androidxmlbase.core.network.transfer

import com.thanhng224.androidxmlbase.core.architecture.AppDispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject

internal class OkHttpFileTransferClient
    @Inject
    internal constructor(
        private val okHttpClient: OkHttpClient,
        private val dispatchers: AppDispatchers,
    ) : FileTransferClient {
        override fun download(
            request: Request,
            destination: File,
        ): Flow<DownloadResult> =
            flow {
                okHttpClient.newCall(request).execute().use { response ->
                    val body =
                        response.body ?: run {
                            emit(TransferResult.Failure("Empty response body"))
                            return@flow
                        }
                    if (!response.isSuccessful) {
                        emit(TransferResult.Failure("HTTP ${response.code}"))
                        return@flow
                    }
                    destination.parentFile?.mkdirs()
                    val totalBytes = body.contentLength()
                    var bytesCopied = 0L
                    body.byteStream().use { input ->
                        destination.outputStream().use { output ->
                            val buffer = ByteArray(FileTransferClient.DEFAULT_STREAM_CHUNK_SIZE_BYTES)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                bytesCopied += read
                                emit(TransferResult.Progress(bytesCopied, totalBytes))
                            }
                        }
                    }
                    emit(TransferResult.Success(destination))
                }
            }.flowOn(dispatchers.io)

        override fun upload(request: Request): Flow<UploadResult> =
            callbackFlow {
                val body = request.body
                val trackedRequest =
                    if (body == null) {
                        request
                    } else {
                        request
                            .newBuilder()
                            .method(
                                request.method,
                                ProgressRequestBody(body) { bytesWritten, totalBytes ->
                                    trySend(TransferResult.Progress(bytesWritten, totalBytes))
                                },
                            ).build()
                    }
                val call = okHttpClient.newCall(trackedRequest)
                val job =
                    launch(dispatchers.io) {
                        try {
                            call.execute().use { response ->
                                val responseBody = response.body?.string()
                                if (response.isSuccessful) {
                                    trySend(
                                        TransferResult.Success(
                                            HttpTransferResponse(
                                                code = response.code,
                                                body = responseBody,
                                            ),
                                        ),
                                    )
                                } else {
                                    trySend(TransferResult.Failure("HTTP ${response.code}"))
                                }
                                close()
                            }
                        } catch (exception: IOException) {
                            trySend(TransferResult.Failure("Network error", exception))
                            close(exception)
                        }
                    }
                awaitClose {
                    call.cancel()
                    job.cancel()
                }
            }

        override fun stream(
            request: Request,
            chunkSizeBytes: Int,
        ): Flow<StreamResult> =
            flow {
                okHttpClient.newCall(request).execute().use { response ->
                    val body =
                        response.body ?: run {
                            emit(TransferResult.Failure("Empty response body"))
                            return@flow
                        }
                    if (!response.isSuccessful) {
                        emit(TransferResult.Failure("HTTP ${response.code}"))
                        return@flow
                    }
                    var bytesRead = 0L
                    body.byteStream().use { input ->
                        val buffer = ByteArray(chunkSizeBytes)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            bytesRead += read
                            emit(TransferResult.Success(StreamChunk(buffer.copyOf(read), bytesRead)))
                        }
                    }
                }
            }.flowOn(dispatchers.io)
    }
