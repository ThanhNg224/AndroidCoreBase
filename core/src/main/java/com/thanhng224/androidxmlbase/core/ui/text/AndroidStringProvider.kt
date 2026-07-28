package com.thanhng224.androidxmlbase.core.ui.text

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AndroidStringProvider
    @Inject
    internal constructor(
        @param:ApplicationContext private val context: Context,
    ) : StringProvider {
        override fun getString(
            @StringRes resId: Int,
        ): String = context.getString(resId)
    }
