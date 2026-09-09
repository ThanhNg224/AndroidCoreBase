package com.thanhng224.androidcorebase.core.network.transfer

import androidx.core.util.AtomicFile
import com.thanhng224.androidcorebase.core.foundation.AppDispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** HTTP statuses that per RFC 9110 must never carry a body, regardless of what the transport reports. */
private val NO_BODY_STATUS_CODES = setOf(204, 205)

private fun Long.toNullableTotalBytes(): Long? = takeIf { it >= 0 }

internal class OkHttpFileTransferClient
    @Inject
    internal constructor(
        private val okHttpClient: OkHttpClient,
        private val dispatchers: AppDispatchers,
    ) : FileTransferClient {
        override fun download(
            request: Request,
            destination: File,
        ): Flow<DownloadEvent> =
            flow {
                okHttpClient.newCall(request).execute().use { response ->
                    val earlyFailure = response.terminalFailureOrNull()
                    if (earlyFailure != null) {
                        emit(TransferEvent.Failed(earlyFailure))
                        return@flow
                    }

                    val body = response.body
                    val atomicFile = AtomicFile(destination)
                    destination.parentFile?.mkdirs()
                    val output =
                        try {
                            atomicFile.startWrite()
                        } catch (e: IOException) {
                            emit(TransferEvent.Failed(TransferError.FileSystem(e)))
                            return@flow
                        }

                    var wroteSuccessfully = false
                    try {
                        val totalBytes = body.contentLength().toNullableTotalBytes()
                        var bytesCopied = 0L
                        body.byteStream().use { input ->
                            val buffer = ByteArray(FileTransferClient.DEFAULT_STREAM_CHUNK_SIZE_BYTES)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                bytesCopied += read
                                emit(TransferEvent.Progress(bytesCopied, totalBytes))
                            }
                        }
                        atomicFile.finishWrite(output)
                        wroteSuccessfully = true
                        emit(TransferEvent.Completed(destination))
                    } catch (e: IOException) {
                        emit(TransferEvent.Failed(TransferError.Network(e)))
                    } finally {
                        if (!wroteSuccessfully) atomicFile.failWrite(output)
                    }
                }
            }.flowOn(dispatchers.io).conflate()

        override fun upload(request: Request): Flow<UploadEvent> =
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
                                    trySend(TransferEvent.Progress(bytesWritten, totalBytes.toNullableTotalBytes()))
                                },
                            ).build()
                    }
                val call = okHttpClient.newCall(trackedRequest)
                val job =
                    launch(dispatchers.io) {
                        try {
                            call.execute().use { response ->
                                if (response.isSuccessful) {
                                    trySend(
                                        TransferEvent.Completed(
                                            HttpTransferMetadata(
                                                code = response.code,
                                                headers = response.headers.toMultimap(),
                                            ),
                                        ),
                                    )
                                } else {
                                    trySend(TransferEvent.Failed(TransferError.Http(response.code)))
                                }
                                close()
                            }
                        } catch (e: IOException) {
                            trySend(TransferEvent.Failed(TransferError.Network(e)))
                            close()
                        }
                    }
                awaitClose {
                    call.cancel()
                    job.cancel()
                }
            }.conflate()

        override fun stream(
            request: Request,
            chunkSizeBytes: Int,
        ): Flow<StreamEvent> {
            require(chunkSizeBytes > 0) { "chunkSizeBytes must be positive, was $chunkSizeBytes" }
            return flow {
                okHttpClient.newCall(request).execute().use { response ->
                    val earlyFailure = response.terminalFailureOrNull()
                    if (earlyFailure != null) {
                        emit(TransferEvent.Failed(earlyFailure))
                        return@flow
                    }

                    try {
                        response.body.byteStream().use { input ->
                            val buffer = ByteArray(chunkSizeBytes)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                emit(TransferEvent.Payload(buffer.copyOf(read)))
                            }
                        }
                        emit(TransferEvent.Completed(Unit))
                    } catch (e: IOException) {
                        emit(TransferEvent.Failed(TransferError.Network(e)))
                    }
                }
            }.flowOn(dispatchers.io)
        }

        /** Null when the response is a real body that should be copied/streamed. */
        private fun Response.terminalFailureOrNull(): TransferError? =
            when {
                !isSuccessful -> TransferError.Http(code)
                code in NO_BODY_STATUS_CODES -> TransferError.EmptyBody
                else -> null
            }
    }
