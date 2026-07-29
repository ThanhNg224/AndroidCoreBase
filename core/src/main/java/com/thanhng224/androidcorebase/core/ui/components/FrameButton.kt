package com.thanhng224.androidcorebase.core.ui.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.thanhng224.androidcorebase.core.R
import com.thanhng224.androidcorebase.core.ui.drawable.DrawableShape
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * `FrameLayout`-based button. The only concrete [ButtonStyleDelegate] consumer ported in this
 * base for now — see the Phase 5 plan for why the reference project's other button variants
 * (`LinearButton`, `CardButton`, ...) are deferred until a real screen needs a different base
 * View/ViewGroup.
 */
public class FrameButton
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        init {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CoreFrameButton, defStyleAttr, 0)
            val shapeOrdinal = typedArray.getInt(R.styleable.CoreFrameButton_coreButtonShape, 0)
            val backgroundColor = typedArray.getColor(R.styleable.CoreFrameButton_coreButtonBackgroundColor, Color.TRANSPARENT)
            val cornerRadiusPx = typedArray.getDimension(R.styleable.CoreFrameButton_coreButtonCornerRadius, 0f)
            val strokeWidthPx = typedArray.getDimension(R.styleable.CoreFrameButton_coreButtonStrokeWidth, 0f)
            val strokeColor = typedArray.getColor(R.styleable.CoreFrameButton_coreButtonStrokeColor, Color.TRANSPARENT)
            typedArray.recycle()

            val shape = if (shapeOrdinal == SHAPE_OVAL_ORDINAL) DrawableShape.OVAL else DrawableShape.RECTANGLE
            ButtonStyleDelegate(
                targetView = this,
                shape = shape,
                backgroundColor = backgroundColor,
                cornerRadiusPx = cornerRadiusPx,
                strokeWidthPx = strokeWidthPx,
                strokeColor = strokeColor,
            ).apply()

            isClickable = true
            isFocusable = true
            minimumHeight =
                max(
                    minimumHeight,
                    resources.displayMetrics.density
                        .times(MIN_TOUCH_TARGET_DP)
                        .roundToInt(),
                )
            ViewCompat.setAccessibilityDelegate(
                this,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: android.view.View,
                        info: AccessibilityNodeInfoCompat,
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = Button::class.java.name
                        info.isClickable = host.isClickable
                        info.isEnabled = host.isEnabled
                    }
                },
            )
        }

        private companion object {
            const val SHAPE_OVAL_ORDINAL = 1
            const val MIN_TOUCH_TARGET_DP = 48
        }
    }
