package com.thanhng224.androidcorebase.core.ui.base

import android.os.Bundle

/**
 * Base activity tailored for Jetpack Compose screens: extends [BaseActivity] to share edge-to-edge,
 * lifecycle, and MVI capabilities while leaving UI rendering to Compose `setContent`.
 */
public abstract class BaseComposeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onSetupComposeContent(savedInstanceState)
    }

    /** Subclasses initialize their Compose `setContent { }` tree here. */
    protected abstract fun onSetupComposeContent(savedInstanceState: Bundle?)
}
