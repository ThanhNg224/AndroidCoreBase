package com.thanhng224.androidcorebase.core.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import com.thanhng224.androidcorebase.core.R

/**
 * Bridges the base XML Material 3 color resources (`core_color_*`) into a Compose [MaterialTheme].
 */
@Composable
public fun AndroidCoreBaseTheme(content: @Composable () -> Unit) {
    val isDark = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    val colorScheme =
        base.copy(
            primary = colorResource(R.color.core_color_primary),
            onPrimary = colorResource(R.color.core_color_on_primary),
            primaryContainer = colorResource(R.color.core_color_primary_container),
            onPrimaryContainer = colorResource(R.color.core_color_on_primary_container),
            secondary = colorResource(R.color.core_color_secondary),
            onSecondary = colorResource(R.color.core_color_on_secondary),
            secondaryContainer = colorResource(R.color.core_color_secondary_container),
            onSecondaryContainer = colorResource(R.color.core_color_on_secondary_container),
            tertiary = colorResource(R.color.core_color_tertiary),
            onTertiary = colorResource(R.color.core_color_on_tertiary),
            tertiaryContainer = colorResource(R.color.core_color_tertiary_container),
            onTertiaryContainer = colorResource(R.color.core_color_on_tertiary_container),
            background = colorResource(R.color.core_color_background),
            onBackground = colorResource(R.color.core_color_on_background),
            surface = colorResource(R.color.core_color_surface),
            onSurface = colorResource(R.color.core_color_on_surface),
            surfaceVariant = colorResource(R.color.core_color_surface_variant),
            onSurfaceVariant = colorResource(R.color.core_color_on_surface_variant),
            outline = colorResource(R.color.core_color_outline),
            outlineVariant = colorResource(R.color.core_color_outline_variant),
            error = colorResource(R.color.core_color_error),
            onError = colorResource(R.color.core_color_on_error),
            errorContainer = colorResource(R.color.core_color_error_container),
            onErrorContainer = colorResource(R.color.core_color_on_error_container),
        )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
