package com.thanhng224.androidcorebase.core.architecture.result

public sealed interface DomainResult<out T> {
    public data class Success<T>(
        val data: T,
    ) : DomainResult<T>

    public data class Error(
        val error: AppError,
    ) : DomainResult<Nothing>
}

/**
 * Transforms a [DomainResult.Success] payload while passing [DomainResult.Error] through
 * untouched. Lets a repository map a data-layer type to a domain entity without unwrapping and
 * rebuilding the result at every call site.
 */
public inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(data))
        is DomainResult.Error -> this
    }

public sealed interface AppError {
    public val cause: Throwable?

    public data class Http(
        val code: Int,
        val serverMessage: String,
    ) : AppError {
        override val cause: Throwable? = null
    }

    public data class Network(
        override val cause: Throwable,
    ) : AppError

    public data class Parse(
        override val cause: Throwable,
    ) : AppError

    public data object EmptyBody : AppError {
        override val cause: Throwable? = null
    }

    /** Business-layer error response (e.g., HTTP 200 response envelope with error code). */
    public data class Business(
        val code: Int,
        val message: String,
    ) : AppError {
        override val cause: Throwable? = null
    }
}
