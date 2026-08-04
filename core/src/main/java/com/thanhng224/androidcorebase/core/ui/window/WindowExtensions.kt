package com.thanhng224.androidcorebase.core.ui.window

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

/**
 * Configure this window to use modern, clean edge-to-edge immersive mode.
 * Hides the status/notification bar and system navigation controls.
 * Swipe from edge reveals the system controls temporarily without resizing the content.
 */
public fun Window.setImmersiveMode(enabled: Boolean) {
    val decorView = decorView
    val controller = WindowCompat.getInsetsController(this, decorView)

    if (enabled) {
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())

        // Extend content behind camera cutout/notch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            attributes = params
        }
    } else {
        controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            attributes = params
        }
    }
}

/**
 * Pads [this] by whatever the system bars and the display cutout occupy on the requested edges, so
 * an edge-to-edge window draws its content clear of them.
 *
 * Required rather than optional: from Android 15 (`targetSdk` 35+) the system lays every window out
 * edge-to-edge and ignores `android:statusBarColor`, so a root view with no inset handling renders
 * underneath the status bar.
 *
 * **Each edge must be handled in exactly one place.** The insets are returned *unconsumed*, so any
 * descendant that does its own inset handling still sees them — Material's `NavigationBarView`
 * (`BottomNavigationView`), `AppBarLayout` and `BottomSheetBehavior` all do. Leaving [bottom] on
 * while a `BottomNavigationView` sits at the window edge pads the bottom twice and leaves a dead
 * strip inside it. Turn off the edges a child already owns; see `MainActivity` for the worked case.
 *
 * Padding is *set*, not accumulated — the listener re-runs on every rotation, keyboard show and
 * cutout change, and adding to existing padding each time would compound it. Padding the layout
 * declares on this view is therefore replaced on a handled edge; declare it on a child instead.
 */
public fun View.applySystemBarInsetsAsPadding(
    left: Boolean = true,
    top: Boolean = true,
    right: Boolean = true,
    bottom: Boolean = true,
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets =
            windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
        view.updatePadding(
            left = if (left) insets.left else view.paddingLeft,
            top = if (top) insets.top else view.paddingTop,
            right = if (right) insets.right else view.paddingRight,
            bottom = if (bottom) insets.bottom else view.paddingBottom,
        )
        windowInsets
    }
}
