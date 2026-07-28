package com.thanhng224.androidxmlbase.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.Flow

abstract class BaseBottomSheetDialogFragment<VB : ViewBinding> : BottomSheetDialogFragment() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed outside the BottomSheet view lifecycle" }

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
}
