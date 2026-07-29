package com.example.androidcorebase.sample.designsystem.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import com.example.androidcorebase.R
import com.example.androidcorebase.databinding.FragmentDesignSystemBinding
import com.example.androidcorebase.sample.designsystem.presentation.state.DesignSystemUiEvent
import com.example.androidcorebase.sample.designsystem.presentation.viewmodel.DesignSystemViewModel
import com.thanhng224.androidcorebase.core.architecture.result.ResultState
import com.thanhng224.androidcorebase.core.architecture.result.fold
import com.thanhng224.androidcorebase.core.ui.base.BaseFragment
import com.thanhng224.androidcorebase.core.ui.base.setThemedContent
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
        // Proves Phase 3's Compose interop: a ComposeView embedded in this XML layout, themed
        // by AndroidCoreBaseTheme instead of falling back to Compose's stock Material purple.
        binding.composeInteropDemo.setThemedContent { ComposeInteropDemo() }

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

@Composable
private fun ComposeInteropDemo() {
    Card(
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Text(
            text = stringResource(R.string.design_system_compose_interop_title),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(dimensionResource(CoreR.dimen.core_space_16)),
        )
        Text(
            text = stringResource(R.string.design_system_compose_interop_body),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.padding(
                    start = dimensionResource(CoreR.dimen.core_space_16),
                    end = dimensionResource(CoreR.dimen.core_space_16),
                    bottom = dimensionResource(CoreR.dimen.core_space_16),
                ),
        )
    }
}
