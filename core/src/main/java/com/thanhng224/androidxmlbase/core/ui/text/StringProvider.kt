package com.thanhng224.androidxmlbase.core.ui.text

import androidx.annotation.StringRes

/** Lets a ViewModel resolve string resources without holding an Activity/View `Context`. */
interface StringProvider {
    fun getString(
        @StringRes resId: Int,
    ): String
}
