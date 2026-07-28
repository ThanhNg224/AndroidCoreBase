package com.thanhng224.androidxmlbase.core.ui.drawable

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import kotlin.math.roundToInt

/** Shapes [ShapeDrawableFactory.buildDrawable] can render. */
enum class DrawableShape {
    RECTANGLE,
    OVAL,
}

/**
 * Builds runtime [GradientDrawable]s for custom Views (buttons, shadow cards, toasts, ...) from
 * plain values, so each component doesn't hand-roll [GradientDrawable] setup individually.
 */
object ShapeDrawableFactory {
    fun buildDrawable(
        shape: DrawableShape,
        cornerRadiusPx: Float,
        fillColor: Int,
        strokeWidthPx: Float = 0f,
        strokeColor: Int = Color.TRANSPARENT,
    ): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape =
            when (shape) {
                DrawableShape.RECTANGLE -> GradientDrawable.RECTANGLE
                DrawableShape.OVAL -> GradientDrawable.OVAL
            }
        drawable.setColor(fillColor)
        if (shape == DrawableShape.RECTANGLE) {
            drawable.cornerRadius = resolveCornerRadiusPx(cornerRadiusPx)
        }
        val resolvedStrokeWidthPx = resolveStrokeWidthPx(strokeWidthPx)
        if (resolvedStrokeWidthPx > 0) {
            drawable.setStroke(resolvedStrokeWidthPx, strokeColor)
        }
        return drawable
    }

    /**
     * Clamps a negative corner radius to zero. [GradientDrawable] otherwise renders a negative
     * radius as an unpredictable sharp corner, which reads as a caller attribute bug rather than
     * an intentional shape.
     */
    internal fun resolveCornerRadiusPx(cornerRadiusPx: Float): Float = cornerRadiusPx.coerceAtLeast(0f)

    /**
     * Rounds a stroke width in pixels to the nearest int for [GradientDrawable.setStroke], while
     * guaranteeing any strictly-positive width still renders at least a 1px stroke instead of
     * rounding away to 0 (which would silently drop the stroke a caller asked for).
     */
    internal fun resolveStrokeWidthPx(strokeWidthPx: Float): Int {
        if (strokeWidthPx <= 0f) return 0
        return strokeWidthPx.roundToInt().coerceAtLeast(1)
    }
}
