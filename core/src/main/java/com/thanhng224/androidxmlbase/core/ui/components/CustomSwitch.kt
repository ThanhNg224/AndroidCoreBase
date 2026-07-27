package com.thanhng224.androidxmlbase.core.ui.components

import android.content.Context
import android.util.AttributeSet

/** Source and XML compatibility shim for [ThemedSwitch]. New layouts must use [ThemedSwitch]. */
@Deprecated(
    message = "Use ThemedSwitch.",
    replaceWith = ReplaceWith("ThemedSwitch(context, attrs, defStyleAttr)"),
)
internal class CustomSwitch
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = com.google.android.material.R.attr.materialSwitchStyle,
    ) : ThemedSwitch(context, attrs, defStyleAttr)
