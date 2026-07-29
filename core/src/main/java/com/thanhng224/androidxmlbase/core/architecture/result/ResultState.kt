package com.thanhng224.androidxmlbase.core.architecture.result

import com.thanhng224.androidxmlbase.core.ui.text.UiText

public sealed interface ResultState<out T> {
    public data object Loading : ResultState<Nothing>

    public data class Success<T>(
        val data: T,
    ) : ResultState<T>

    public data class Error(
        val message: UiText,
        val cause: Throwable? = null,
    ) : ResultState<Nothing>
}

public inline fun <T, R> ResultState<T>.fold(
    onLoading: () -> R,
    onSuccess: (T) -> R,
    onError: (UiText, Throwable?) -> R,
): R =
    when (this) {
        is ResultState.Loading -> onLoading()
        is ResultState.Success -> onSuccess(data)
        is ResultState.Error -> onError(message, cause)
    }
