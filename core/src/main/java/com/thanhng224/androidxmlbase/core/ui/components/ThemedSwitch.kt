package com.thanhng224.androidxmlbase.core.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.thanhng224.androidxmlbase.core.R

/** [MaterialSwitch] wrapped to apply design system color tokens. */
internal open class ThemedSwitch
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = com.google.android.material.R.attr.materialSwitchStyle,
    ) : MaterialSwitch(context, attrs, defStyleAttr) {
        init {
            val checkedTrackColor = ContextCompat.getColor(context, R.color.core_color_primary)
            val uncheckedTrackColor = ContextCompat.getColor(context, R.color.core_color_surface_variant)

            val checkedThumbColor = ContextCompat.getColor(context, R.color.core_color_surface)
            val uncheckedThumbColor = ContextCompat.getColor(context, R.color.core_color_outline)

            trackTintList = checkedStateList(checkedTrackColor, uncheckedTrackColor)
            trackDecorationTintList = checkedStateList(checkedTrackColor, uncheckedTrackColor)
            thumbTintList = checkedStateList(checkedThumbColor, uncheckedThumbColor)

            // Prevent text overlap
            val density = context.resources.displayMetrics.density
            switchPadding = (16 * density).toInt()

            textOn = ""
            textOff = ""
            showText = false
        }

        private fun checkedStateList(
            checkedColor: Int,
            uncheckedColor: Int,
        ): ColorStateList =
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(), // Unchecked fallback
                ),
                intArrayOf(checkedColor, uncheckedColor),
            )
    }
