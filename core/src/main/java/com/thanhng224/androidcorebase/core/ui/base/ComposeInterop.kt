package com.thanhng224.androidcorebase.core.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.thanhng224.androidcorebase.core.ui.theme.AndroidCoreBaseTheme

/**
 * Sets [content] on this [ComposeView], wrapped in [AndroidCoreBaseTheme] and configured with
 * [ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed] for XML embedding.
 */
public fun ComposeView.setThemedContent(content: @Composable () -> Unit) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        AndroidCoreBaseTheme(content)
    }
}
