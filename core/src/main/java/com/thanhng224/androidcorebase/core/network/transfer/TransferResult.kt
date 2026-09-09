package com.thanhng224.androidcorebase.core.network.transfer

import java.io.File
import java.io.IOException

public sealed interface TransferEvent<out P, out R> {
    public data class Progress(
        public val bytesTransferred: Long,
        public val totalBytes: Long?,
    ) : TransferEvent<Nothing, Nothing>

    public data class Payload<P>(
        public val value: P,
    ) : TransferEvent<P, Nothing>

    public data class Completed<R>(
        public val value: R,
    ) : TransferEvent<Nothing, R>

    public data class Failed(
        public val error: TransferError,
    ) : TransferEvent<Nothing, Nothing>
}

public sealed interface TransferError {
    public data class Http(
        public val code: Int,
    ) : TransferError

    public data class Network(
        public val cause: IOException,
    ) : TransferError

    public data class FileSystem(
        public val cause: IOException,
    ) : TransferError

    public data object EmptyBody : TransferError
}

public data class HttpTransferMetadata(
    public val code: Int,
    public val headers: Map<String, List<String>>,
)

public typealias DownloadEvent = TransferEvent<Nothing, File>
public typealias UploadEvent = TransferEvent<Nothing, HttpTransferMetadata>
public typealias StreamEvent = TransferEvent<ByteArray, Unit>
