package com.thanhng224.androidcorebase.core.network

import java.io.IOException

public sealed interface ApiFailure {
    public data class Http(
        public val code: Int,
        public val serverMessage: String?,
    ) : ApiFailure

    public data class Network(
        public val cause: IOException,
    ) : ApiFailure

    public data class Serialization(
        public val cause: Throwable,
    ) : ApiFailure

    public data object EmptyBody : ApiFailure
}

public sealed interface ApiResult<out T> {
    public data class Success<T>(
        public val value: T,
    ) : ApiResult<T>

    public data class Failure(
        public val error: ApiFailure,
    ) : ApiResult<Nothing>
}
