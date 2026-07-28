package com.thanhng224.androidxmlbase.core.architecture.result

import com.thanhng224.androidxmlbase.core.ui.text.UiText

sealed interface ResultState<out T> {
    data object Loading : ResultState<Nothing>

    data class Success<T>(
        val data: T,
    ) : ResultState<T>

    data class Error(
        val message: UiText,
        val cause: Throwable? = null,
    ) : ResultState<Nothing>
}

inline fun <T, R> ResultState<T>.fold(
    onLoading: () -> R,
    onSuccess: (T) -> R,
    onError: (UiText, Throwable?) -> R,
): R =
    when (this) {
        is ResultState.Loading -> onLoading()
        is ResultState.Success -> onSuccess(data)
        is ResultState.Error -> onError(message, cause)
    }
