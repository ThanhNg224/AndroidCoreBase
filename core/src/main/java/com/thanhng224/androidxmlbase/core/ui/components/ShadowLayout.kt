package com.thanhng224.androidxmlbase.core.ui.components

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.thanhng224.androidxmlbase.core.R

/**
 * A `FrameLayout` that draws a soft platform shadow behind its content via elevation + a rounded
 * outline, rather than hand-rolling a blurred/offset shape on a software layer. Elevation
 * shadows are hardware-accelerated and always geometrically correct; with no device available to
 * tune a hand-drawn blur against this phase, this is the technique that's clearly correct rather
 * than merely clever. Depth is controlled with the standard `android:elevation` attribute — no
 * custom attr is added for it.
 */
class ShadowLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val cornerRadiusPx: Float

        init {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CoreShadowLayout, defStyleAttr, 0)
            cornerRadiusPx = typedArray.getDimension(R.styleable.CoreShadowLayout_coreShadowCornerRadius, 0f)
            val backgroundColor =
                typedArray.getColor(
                    R.styleable.CoreShadowLayout_coreShadowBackgroundColor,
                    ContextCompat.getColor(context, R.color.core_color_surface),
                )
            typedArray.recycle()

            setBackgroundColor(backgroundColor)
            clipToOutline = true
            outlineProvider =
                object : ViewOutlineProvider() {
                    override fun getOutline(
                        view: View,
                        outline: Outline,
                    ) {
                        outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
                    }
                }
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            // Ensure outline updates if platform doesn't invalidate automatically
            invalidateOutline()
        }
    }
