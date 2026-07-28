package com.example.androidxmlbase.sample.designsystem.presentation.viewmodel

import com.example.androidxmlbase.sample.designsystem.presentation.state.DesignSystemUiEvent
import com.thanhng224.androidxmlbase.core.architecture.result.ResultState
import com.thanhng224.androidxmlbase.core.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * No [com.example.androidxmlbase.testutil.MainDispatcherRule] needed: this ViewModel never calls
 * `viewModelScope.launch` (no effect, no async work), so `state.value` can be read synchronously
 * right after `onEvent`.
 */
class DesignSystemViewModelTest {
    @Test
    fun `initial state starts as Loading`() {
        val viewModel = DesignSystemViewModel()

        assertEquals(ResultState.Loading, viewModel.state.value.demoResult)
    }

    @Test
    fun `ShowLoadingClicked sets demoResult to Loading`() {
        val viewModel = DesignSystemViewModel()
        viewModel.onEvent(DesignSystemUiEvent.ShowSuccessClicked)

        viewModel.onEvent(DesignSystemUiEvent.ShowLoadingClicked)

        assertEquals(ResultState.Loading, viewModel.state.value.demoResult)
    }

    @Test
    fun `ShowSuccessClicked sets demoResult to Success`() {
        val viewModel = DesignSystemViewModel()

        viewModel.onEvent(DesignSystemUiEvent.ShowSuccessClicked)

        assertEquals(ResultState.Success(Unit), viewModel.state.value.demoResult)
    }

    @Test
    fun `ShowErrorClicked sets demoResult to an Error with a sample message`() {
        val viewModel = DesignSystemViewModel()

        viewModel.onEvent(DesignSystemUiEvent.ShowErrorClicked)

        assertEquals(
            ResultState.Error(UiText.StringResource(com.example.androidxmlbase.R.string.design_system_error_sample)),
            viewModel.state.value.demoResult,
        )
    }
}
