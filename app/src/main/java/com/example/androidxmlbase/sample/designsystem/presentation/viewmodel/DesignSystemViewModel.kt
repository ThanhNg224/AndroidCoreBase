package com.example.androidxmlbase.sample.designsystem.presentation.viewmodel

import com.example.androidxmlbase.R
import com.example.androidxmlbase.sample.designsystem.presentation.state.DesignSystemUiEvent
import com.example.androidxmlbase.sample.designsystem.presentation.state.DesignSystemUiState
import com.thanhng224.androidxmlbase.core.architecture.StateViewModel
import com.thanhng224.androidxmlbase.core.architecture.UiEffect
import com.thanhng224.androidxmlbase.core.architecture.result.ResultState
import com.thanhng224.androidxmlbase.core.ui.text.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Pure UI-state demo for the [ResultState] loading/success/error showcase — no repository/use
 * case, no async work, so `onEvent` sets state synchronously. Parameterized directly with the
 * base [UiEffect] marker (rather than a dedicated `DesignSystemUiEffect`) since this screen never
 * emits a one-shot effect.
 */
@HiltViewModel
class DesignSystemViewModel
    @Inject
    constructor() :
    StateViewModel<DesignSystemUiState, DesignSystemUiEvent, UiEffect>(DesignSystemUiState()) {
        override fun onEvent(event: DesignSystemUiEvent) {
            when (event) {
                is DesignSystemUiEvent.ShowLoadingClicked -> setState { copy(demoResult = ResultState.Loading) }
                is DesignSystemUiEvent.ShowSuccessClicked -> setState { copy(demoResult = ResultState.Success(Unit)) }
                is DesignSystemUiEvent.ShowErrorClicked -> {
                    setState {
                        copy(
                            demoResult = ResultState.Error(UiText.StringResource(R.string.design_system_error_sample)),
                        )
                    }
                }
            }
        }
    }
