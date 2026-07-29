package com.thanhng224.androidcorebase.core.ui.components

import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.thanhng224.androidcorebase.core.R
import com.thanhng224.androidcorebase.core.ui.drawable.DrawableShape
import com.thanhng224.androidcorebase.core.ui.drawable.ShapeDrawableFactory

/** Status message surface backed by a styled [Snackbar] for foreground reliability. */
public object StyledSnackbar {
    public fun show(
        anchorView: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
    ): Snackbar {
        val context = anchorView.context
        val density = context.resources.displayMetrics.density

        val snackbar = Snackbar.make(anchorView, message, duration)
        snackbar.setTextColor(ContextCompat.getColor(context, R.color.core_color_surface))
        snackbar.view.backgroundTintList = null
        snackbar.view.background =
            ShapeDrawableFactory.buildDrawable(
                shape = DrawableShape.RECTANGLE,
                cornerRadiusPx = CORNER_RADIUS_DP * density,
                fillColor = ContextCompat.getColor(context, R.color.core_color_on_surface),
            )
        snackbar.show()
        return snackbar
    }

    private const val CORNER_RADIUS_DP = 8f
}
