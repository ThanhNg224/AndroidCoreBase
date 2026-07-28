package com.thanhng224.androidxmlbase.core.ui.base

import android.view.View
import com.thanhng224.androidxmlbase.core.architecture.result.ResultState
import com.thanhng224.androidxmlbase.core.ui.text.UiText

/** Visibility-only projection of a [ResultState], independent of any View. */
data class ResultRenderState(
    val isLoadingVisible: Boolean,
    val isContentVisible: Boolean,
    val isErrorVisible: Boolean,
    val errorMessage: UiText?,
)

fun <T> ResultState<T>.toRenderState(): ResultRenderState =
    when (this) {
        is ResultState.Loading ->
            ResultRenderState(
                isLoadingVisible = true,
                isContentVisible = false,
                isErrorVisible = false,
                errorMessage = null,
            )
        is ResultState.Success ->
            ResultRenderState(
                isLoadingVisible = false,
                isContentVisible = true,
                isErrorVisible = false,
                errorMessage = null,
            )
        is ResultState.Error ->
            ResultRenderState(
                isLoadingVisible = false,
                isContentVisible = false,
                isErrorVisible = true,
                errorMessage = message,
            )
    }

/**
 * Android-only glue: applies this render state's visibility booleans to real Views. Not
 * unit-tested directly (needs a real [View]); the mapping it applies is covered by
 * [toRenderState]'s tests.
 */
fun ResultRenderState.applyVisibilityTo(
    loadingView: View,
    contentView: View,
    errorView: View,
) {
    loadingView.visibility = if (isLoadingVisible) View.VISIBLE else View.GONE
    contentView.visibility = if (isContentVisible) View.VISIBLE else View.GONE
    errorView.visibility = if (isErrorVisible) View.VISIBLE else View.GONE
}
