package com.thanhng224.androidxmlbase.core.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.thanhng224.androidxmlbase.core.architecture.result.ResultState
import com.thanhng224.androidxmlbase.core.ui.responsive.ResponsiveConfig
import com.thanhng224.androidxmlbase.core.ui.responsive.ResponsiveContextWrapper
import com.thanhng224.androidxmlbase.core.ui.window.applySystemBarInsetsAsPadding
import com.thanhng224.androidxmlbase.core.ui.window.setImmersiveMode
import kotlinx.coroutines.flow.Flow

/**
 * Common base for XML + ViewBinding activities: inflates [VB], applies the responsive
 * `attachBaseContext` wrapping every screen needs, handles edge-to-edge window insets, and offers
 * [collectOnStarted] for lifecycle-safe Flow collection.
 */
public abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed before onCreate() completed" }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    protected open val responsiveConfig: ResponsiveConfig = ResponsiveConfig()
    protected open val useImmersiveMode: Boolean = false

    /**
     * Whether the binding root gets system-bar insets applied as padding.
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ResponsiveContextWrapper.wrap(newBase, responsiveConfig))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before setContentView: enableEdgeToEdge() adjusts the window before the decor view is
        // laid out, and running it afterwards leaves the first frame with the wrong fitting.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (useImmersiveMode) {
            window.setImmersiveMode(true)
        }
        bindingOrNull = inflateBinding(layoutInflater)
        setContentView(binding.root)
        if (applyInsetsToRoot && !useImmersiveMode) {
            binding.root.applySystemBarInsetsAsPadding()
        }
        onBindingReady(savedInstanceState)
    }

    /** Subclasses do their view/ViewModel wiring here instead of overriding `onCreate`. */
    protected abstract fun onBindingReady(savedInstanceState: Bundle?)

    override fun onDestroy() {
        super.onDestroy()
        bindingOrNull = null
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
