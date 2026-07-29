package com.thanhng224.androidxmlbase.core.architecture.result

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

    /**
     * The transport succeeded but the server rejected the request at the business layer — the
     * common "HTTP 200 with an error code in the response envelope" shape. Distinct from [Http],
     * which carries a transport-level status, because callers usually branch on [code] to decide
     * whether the failure is recoverable.
     */
    public data class Business(
        val code: Int,
        val message: String,
    ) : AppError {
        override val cause: Throwable? = null
    }
}
