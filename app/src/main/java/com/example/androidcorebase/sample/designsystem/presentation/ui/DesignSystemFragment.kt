package com.example.androidcorebase.sample.designsystem.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.androidcorebase.R
import com.example.androidcorebase.databinding.FragmentDesignSystemBinding
import com.example.androidcorebase.sample.designsystem.presentation.state.DesignSystemUiEvent
import com.example.androidcorebase.sample.designsystem.presentation.viewmodel.DesignSystemViewModel
import com.thanhng224.androidcorebase.core.architecture.result.ResultState
import com.thanhng224.androidcorebase.core.architecture.result.fold
import com.thanhng224.androidcorebase.core.ui.base.BaseFragment
import com.thanhng224.androidcorebase.core.ui.base.toRenderState
import com.thanhng224.androidcorebase.core.ui.components.StyledSnackbar
import com.thanhng224.androidcorebase.core.ui.text.resolve
import dagger.hilt.android.AndroidEntryPoint
import com.thanhng224.androidcorebase.core.R as CoreR

@AndroidEntryPoint
class DesignSystemFragment : BaseFragment<FragmentDesignSystemBinding>() {
    private val viewModel: DesignSystemViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentDesignSystemBinding = FragmentDesignSystemBinding.inflate(inflater, container, false)

    override fun onBindingReady(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        binding.btnShowSnackbar.setOnClickListener {
            StyledSnackbar.show(binding.root, getString(R.string.design_system_snackbar_message))
        }
        binding.btnShowLoading.setOnClickListener {
            viewModel.onEvent(DesignSystemUiEvent.ShowLoadingClicked)
        }
        binding.btnShowSuccess.setOnClickListener {
            viewModel.onEvent(DesignSystemUiEvent.ShowSuccessClicked)
        }
        binding.btnShowError.setOnClickListener {
            viewModel.onEvent(DesignSystemUiEvent.ShowErrorClicked)
        }

        observeState()
    }

    private fun observeState() {
        viewModel.state.collectOnStarted { state -> render(state.demoResult) }
    }

    private fun render(result: ResultState<Unit>) {
        val renderState = result.toRenderState()
        binding.progressDemoResult.visibility = if (renderState.isLoadingVisible) View.VISIBLE else View.GONE
        binding.tvDemoResult.text =
            result.fold(
                onLoading = { getString(CoreR.string.core_design_system_result_loading) },
                onSuccess = { getString(R.string.design_system_result_success) },
                onError = { message, _ -> message.resolve(requireContext()) },
            )
    }
}
