package com.thanhng224.androidcorebase.core.ui.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.thanhng224.androidcorebase.core.ui.theme.AndroidCoreBaseTheme

/**
 * Base activity for screens rendered entirely in Jetpack Compose, wrapping [Content] in
 * [AndroidCoreBaseTheme].
 */
public abstract class BaseComposeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidCoreBaseTheme {
                Content()
            }
        }
    }

    /** Subclasses render their Compose UI here. */
    @Composable
    protected abstract fun Content()
}
