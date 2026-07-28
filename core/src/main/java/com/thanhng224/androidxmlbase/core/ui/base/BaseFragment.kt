package com.thanhng224.androidxmlbase.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.thanhng224.androidxmlbase.core.architecture.result.ResultState
import kotlinx.coroutines.flow.Flow

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed outside the Fragment view lifecycle" }

    protected abstract fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingOrNull = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        onBindingReady(view, savedInstanceState)
    }

    protected abstract fun onBindingReady(
        view: View,
        savedInstanceState: Bundle?,
    )

    override fun onDestroyView() {
        bindingOrNull = null
        super.onDestroyView()
    }

    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) = collectOnStartedBy(viewLifecycleOwner, action)

    /**
     * Binds a [ResultState] Flow, displaying a full-screen loading overlay on Loading,
     * showing an error dialog on Error, and executing [onSuccess] when data is loaded.
     */
    protected fun <T> bindResultState(
        flow: Flow<ResultState<T>>,
        onSuccess: (T) -> Unit,
    ) {
        flow.collectOnStarted { result ->
            val root = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return@collectOnStarted
            renderResultState(
                result = result,
                contentRoot = root,
                dialogHost = childFragmentManager,
                onSuccess = onSuccess,
            )
        }
    }
}
