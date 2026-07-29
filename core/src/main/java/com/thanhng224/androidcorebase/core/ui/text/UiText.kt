package com.thanhng224.androidcorebase.core.ui.text

import android.content.Context
import androidx.annotation.StringRes

/** A UI message that can remain localized until the view renders it. */
public sealed interface UiText {
    public data class DynamicString(
        val value: String,
    ) : UiText

    public data class StringResource(
        @param:StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : UiText
}

@Suppress("SpreadOperator")
public fun UiText.resolve(context: Context): String =
    when (this) {
        is UiText.DynamicString -> value
        is UiText.StringResource -> context.getString(resId, *formatArgs.toTypedArray())
    }
