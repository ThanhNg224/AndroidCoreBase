package com.thanhng224.androidcorebase.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import com.thanhng224.androidcorebase.core.ui.window.applySystemBarInsetsAsPadding

/**
 * Common base for XML + ViewBinding activities: inflates [VB], applies system bar insets to
 * [binding.root], and offers [onBindingReady] for subclass initialization.
 */
public abstract class BaseBindingActivity<VB : ViewBinding> : BaseActivity() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed before onCreate() completed" }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
