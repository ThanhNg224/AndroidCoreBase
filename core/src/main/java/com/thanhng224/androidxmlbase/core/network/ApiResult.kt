package com.thanhng224.androidxmlbase.core.network

sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T,
    ) : ApiResult<T>

    data class HttpError(
        val code: Int,
        val message: String,
    ) : ApiResult<Nothing>

    data class NetworkError(
        val cause: Throwable,
    ) : ApiResult<Nothing>

    data class ParseError(
        val cause: Throwable,
    ) : ApiResult<Nothing>

    data object EmptyBody : ApiResult<Nothing>
}
