package com.thanhng224.androidxmlbase.core.architecture.result

sealed interface DomainResult<out T> {
    data class Success<T>(
        val data: T,
    ) : DomainResult<T>

    data class Error(
        val error: AppError,
    ) : DomainResult<Nothing>
}

sealed interface AppError {
    val cause: Throwable?

    data class Http(
        val code: Int,
        val serverMessage: String,
    ) : AppError {
        override val cause: Throwable? = null
    }

    data class Network(
        override val cause: Throwable,
    ) : AppError

    data class Parse(
        override val cause: Throwable,
    ) : AppError

    data object EmptyBody : AppError {
        override val cause: Throwable? = null
    }
}
