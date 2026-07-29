package com.thanhng224.androidcorebase.core.ui.base

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.thanhng224.androidcorebase.core.architecture.result.ResultState
import com.thanhng224.androidcorebase.core.ui.window.applySystemBarInsetsAsPadding
import com.thanhng224.androidcorebase.core.ui.window.setImmersiveMode
import kotlinx.coroutines.flow.Flow

/**
 * Neutral base activity for all screens (XML ViewBinding and Jetpack Compose): handles
 * edge-to-edge window insets, immersive mode, and offers [collectOnStarted] and [bindResultState]
 * for lifecycle-safe Flow collection.
 */
public abstract class BaseActivity : AppCompatActivity() {
    protected open val useImmersiveMode: Boolean = false

    /**
     * Whether the root view gets system-bar insets applied as padding.
     *
     * On by default because Android 15+ lays every window out edge-to-edge regardless: a screen
     * that opts out without handling insets itself draws under the status bar. Override to `false`
     * only when the layout consumes insets on its own (a `CollapsingToolbarLayout`, a full-bleed
     * image, a child calling [applySystemBarInsetsAsPadding] at a different level).
     *
     * Ignored when [useImmersiveMode] is `true` — the bars are hidden there, so reserving space
     * for them would leave a dead strip.
     */
    protected open val applyInsetsToRoot: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before setContentView: enableEdgeToEdge() adjusts the window before the decor view is
        // laid out, and running it afterwards leaves the first frame with the wrong fitting.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (useImmersiveMode) {
            window.setImmersiveMode(true)
        }
    }

    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) {
        collectOnStartedBy(this@BaseActivity, action)
    }

    /**
     * Binds a [ResultState] Flow, displaying a full-screen loading overlay on Loading,
     * showing an error dialog on Error, and executing [onSuccess] when data is loaded.
     */
    protected fun <T> bindResultState(
        flow: Flow<ResultState<T>>,
        onSuccess: (T) -> Unit,
    ) {
        flow.collectOnStarted { result ->
            renderResultState(
                result = result,
                contentRoot = findViewById(android.R.id.content),
                dialogHost = supportFragmentManager,
                onSuccess = onSuccess,
            )
        }
    }
}
