package com.thanhng224.androidcorebase.core.network

public sealed interface ApiResult<out T> {
    public data class Success<T>(
        val data: T,
    ) : ApiResult<T>

    public data class HttpError(
        val code: Int,
        val message: String,
    ) : ApiResult<Nothing>

    public data class NetworkError(
        val cause: Throwable,
    ) : ApiResult<Nothing>

    public data class ParseError(
        val cause: Throwable,
    ) : ApiResult<Nothing>

    public data object EmptyBody : ApiResult<Nothing>
}
