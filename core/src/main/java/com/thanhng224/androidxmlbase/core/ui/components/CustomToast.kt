package com.thanhng224.androidxmlbase.core.ui.components

import android.view.View
import com.google.android.material.snackbar.Snackbar

/** Source compatibility shim for [StyledSnackbar]. New code must use [StyledSnackbar]. */
@Deprecated(
    message = "Use StyledSnackbar.",
    replaceWith = ReplaceWith("StyledSnackbar"),
)
internal object CustomToast {
    fun show(
        anchorView: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
    ): Snackbar = StyledSnackbar.show(anchorView, message, duration)
}
