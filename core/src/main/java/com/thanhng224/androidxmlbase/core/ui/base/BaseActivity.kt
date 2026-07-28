package com.thanhng224.androidxmlbase.core.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.thanhng224.androidxmlbase.core.architecture.result.ResultState
import com.thanhng224.androidxmlbase.core.ui.responsive.ResponsiveConfig
import com.thanhng224.androidxmlbase.core.ui.responsive.ResponsiveContextWrapper
import com.thanhng224.androidxmlbase.core.ui.window.setImmersiveMode
import kotlinx.coroutines.flow.Flow

/**
 * Common base for XML + ViewBinding activities: inflates [VB], applies the responsive
 * `attachBaseContext` wrapping every screen needs, and offers
 * [collectOnStarted] for lifecycle-safe Flow collection.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed before onCreate() completed" }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    protected open val responsiveConfig: ResponsiveConfig = ResponsiveConfig()
    protected open val useImmersiveMode: Boolean = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ResponsiveContextWrapper.wrap(newBase, responsiveConfig))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (useImmersiveMode) {
            window.setImmersiveMode(true)
        }
        bindingOrNull = inflateBinding(layoutInflater)
        setContentView(binding.root)
        onBindingReady(savedInstanceState)
    }

    /** Subclasses do their view/ViewModel wiring here instead of overriding `onCreate`. */
    protected abstract fun onBindingReady(savedInstanceState: Bundle?)

    override fun onDestroy() {
        super.onDestroy()
        bindingOrNull = null
    }

    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) = collectOnStartedBy(this@BaseActivity, action)

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
