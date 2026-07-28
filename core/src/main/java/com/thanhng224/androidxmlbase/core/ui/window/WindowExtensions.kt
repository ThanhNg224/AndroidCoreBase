package com.thanhng224.androidxmlbase.core.ui.window

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Configure this window to use modern, clean edge-to-edge immersive mode.
 * Hides the status/notification bar and system navigation controls.
 * Swipe from edge reveals the system controls temporarily without resizing the content.
 */
fun Window.setImmersiveMode(enabled: Boolean) {
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
