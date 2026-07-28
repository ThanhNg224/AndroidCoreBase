package com.thanhng224.androidxmlbase.core.navigation

import android.content.Intent

enum class TransitionType {
    DEFAULT,
    NONE,
    SLIDE_HORIZONTAL,
    FADE,
}

data class NavigationOptions(
    val clearTask: Boolean = false,
    val singleTop: Boolean = false,
    val noAnimation: Boolean = false,
    val transitionType: TransitionType = if (noAnimation) TransitionType.NONE else TransitionType.DEFAULT,
) {
    fun toIntentFlags(): Int {
        var flags = 0
        if (clearTask) {
            flags = 0 or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        if (singleTop) {
            flags = flags or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        if (noAnimation) {
            flags = flags or Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        return flags
    }
}
