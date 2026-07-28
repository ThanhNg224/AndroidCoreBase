package com.thanhng224.androidxmlbase.core.ui.responsive

import android.content.Context
import android.content.res.Configuration

object ResponsiveContextWrapper {
    fun wrap(
        context: Context,
        config: ResponsiveConfig,
    ): Context {
        if (!config.enabled) return context
        val configuration = Configuration(context.resources.configuration)
        val clamped =
            configuration.smallestScreenWidthDp.coerceIn(
                config.minSmallestScreenWidthDp,
                config.maxSmallestScreenWidthDp,
            )
        configuration.smallestScreenWidthDp = clamped
        return context.createConfigurationContext(configuration)
    }
}
