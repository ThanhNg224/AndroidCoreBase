package com.thanhng224.androidxmlbase.core.ui.components

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.ColorUtils
import com.thanhng224.androidxmlbase.core.ui.drawable.DrawableShape
import com.thanhng224.androidxmlbase.core.ui.drawable.ShapeDrawableFactory

/**
 * Shared background/ripple logic for button-style custom Views. Framework-attribute-agnostic by
 * design: callers parse their own `AttributeSet` via `obtainStyledAttributes` and hand this
 * delegate plain resolved values, so this class stays the reusable part once more button
 * variants (`CardButton`, `LinearButton`, ...) are added.
 */
class ButtonStyleDelegate(
    private val targetView: View,
    private val shape: DrawableShape,
    private val backgroundColor: Int,
    private val cornerRadiusPx: Float,
    private val strokeWidthPx: Float = 0f,
    private val strokeColor: Int = Color.TRANSPARENT,
) {
    /** Builds the shape drawable and applies it, wrapped in a themed ripple, as the target's background. */
    fun apply() {
        val shapeDrawable =
            ShapeDrawableFactory.buildDrawable(
                shape = shape,
                cornerRadiusPx = cornerRadiusPx,
                fillColor = backgroundColor,
                strokeWidthPx = strokeWidthPx,
                strokeColor = strokeColor,
            )
        targetView.background =
            RippleDrawable(
                ColorStateList.valueOf(resolveRippleColor()),
                shapeDrawable,
                shapeDrawable,
            )
    }

    /**
     * Resolves the theme's standard pressed-state color (`colorControlHighlight`) so the ripple
     * matches whatever Material/AppCompat theme is active, instead of hardcoding a tint that
     * could be invisible against some background colors.
     */
    private fun resolveRippleColor(): Int {
        val typedValue = TypedValue()
        val resolved =
            targetView.context.theme.resolveAttribute(
                android.R.attr.colorControlHighlight,
                typedValue,
                true,
            )
        return if (resolved) typedValue.data else ColorUtils.setAlphaComponent(Color.BLACK, FALLBACK_RIPPLE_ALPHA)
    }

    private companion object {
        // Fallback alpha if theme lacks colorControlHighlight
        const val FALLBACK_RIPPLE_ALPHA = 51 // ~20% opacity
    }
}
