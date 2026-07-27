@file:Suppress("DEPRECATION")

package com.thanhng224.androidxmlbase.core.ui.util

import android.app.Activity
import android.os.Bundle
import android.view.Window
import androidx.fragment.app.Fragment
import com.thanhng224.androidxmlbase.core.ui.drawable.DrawableShape
import com.thanhng224.androidxmlbase.core.ui.drawable.ShapeDrawableFactory
import kotlin.properties.ReadOnlyProperty
import com.thanhng224.androidxmlbase.core.navigation.getTyped as navigationGetTyped
import com.thanhng224.androidxmlbase.core.ui.window.setImmersiveMode as applyImmersiveMode

/** Compatibility aliases for APIs moved out of the former `core.ui.util` catch-all package. */
@Deprecated("Use DrawableShape from core.ui.drawable.")
internal typealias Shape = DrawableShape

@Deprecated("Use IntentExtraDelegate from core.navigation.")
internal typealias IntentExtraDelegate<T> = com.thanhng224.androidxmlbase.core.navigation.IntentExtraDelegate<T>

@Deprecated("Use IntentExtraNullableDelegate from core.navigation.")
internal typealias IntentExtraNullableDelegate<T> = com.thanhng224.androidxmlbase.core.navigation.IntentExtraNullableDelegate<T>

@Deprecated("Use FragmentArgumentDelegate from core.navigation.")
internal typealias FragmentArgumentDelegate<T> = com.thanhng224.androidxmlbase.core.navigation.FragmentArgumentDelegate<T>

@Deprecated("Use FragmentArgumentNullableDelegate from core.navigation.")
internal typealias FragmentArgumentNullableDelegate<T> = com.thanhng224.androidxmlbase.core.navigation.FragmentArgumentNullableDelegate<T>

/** Compatibility facade for [ShapeDrawableFactory]. New code must use the factory directly. */
@Deprecated(
    message = "Use ShapeDrawableFactory from core.ui.drawable.",
    replaceWith = ReplaceWith("ShapeDrawableFactory"),
)
internal object ShapeUtils {
    fun buildDrawable(
        shape: DrawableShape,
        cornerRadiusPx: Float,
        fillColor: Int,
        strokeWidthPx: Float = 0f,
        strokeColor: Int = android.graphics.Color.TRANSPARENT,
    ) = ShapeDrawableFactory.buildDrawable(
        shape = shape,
        cornerRadiusPx = cornerRadiusPx,
        fillColor = fillColor,
        strokeWidthPx = strokeWidthPx,
        strokeColor = strokeColor,
    )
}

@Deprecated("Use intentExtra from core.navigation.")
internal inline fun <reified T> intentExtra(
    key: String,
    defaultValue: T? = null,
): ReadOnlyProperty<Activity, T> =
    com.thanhng224.androidxmlbase.core.navigation
        .intentExtra(key, defaultValue)

@Deprecated("Use intentExtraNullable from core.navigation.")
internal inline fun <reified T> intentExtraNullable(key: String): ReadOnlyProperty<Activity, T?> =
    com.thanhng224.androidxmlbase.core.navigation
        .intentExtraNullable(key)

@Deprecated("Use fragmentArg from core.navigation.")
internal inline fun <reified T> fragmentArg(
    key: String,
    defaultValue: T? = null,
): ReadOnlyProperty<Fragment, T> =
    com.thanhng224.androidxmlbase.core.navigation
        .fragmentArg(key, defaultValue)

@Deprecated("Use fragmentArgNullable from core.navigation.")
internal inline fun <reified T> fragmentArgNullable(key: String): ReadOnlyProperty<Fragment, T?> =
    com.thanhng224.androidxmlbase.core.navigation
        .fragmentArgNullable(key)

@Deprecated("Use getTyped from core.navigation.")
internal fun <T> Bundle.getTyped(
    key: String,
    clazz: Class<T>,
): T? = this.navigationGetTyped(key, clazz)

@Deprecated("Use setImmersiveMode from core.ui.window.")
internal fun Window.setImmersiveMode(enabled: Boolean) = applyImmersiveMode(enabled)
