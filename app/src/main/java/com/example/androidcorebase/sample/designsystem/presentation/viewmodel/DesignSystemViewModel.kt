package com.example.androidcorebase.sample.designsystem.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.androidcorebase.R
import com.example.androidcorebase.sample.designsystem.presentation.state.DesignSystemDemoState
import com.example.androidcorebase.sample.designsystem.presentation.state.DesignSystemUiEvent
import com.example.androidcorebase.sample.designsystem.presentation.state.DesignSystemUiState
import com.thanhng224.androidcorebase.core.ui.text.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Pure UI-state demo for the loading/success/error showcase — no repository/use case, no async
 * work, so `onEvent` sets state synchronously.
 */
@HiltViewModel
class DesignSystemViewModel
    @Inject
    constructor() : ViewModel() {
        private val mutableState = MutableStateFlow(DesignSystemUiState())
        val state: StateFlow<DesignSystemUiState> = mutableState.asStateFlow()

        fun onEvent(event: DesignSystemUiEvent) {
            when (event) {
                is DesignSystemUiEvent.ShowLoadingClicked ->
                    mutableState.update { it.copy(demoResult = DesignSystemDemoState.Loading) }
                is DesignSystemUiEvent.ShowSuccessClicked ->
                    mutableState.update { it.copy(demoResult = DesignSystemDemoState.Success) }
                is DesignSystemUiEvent.ShowErrorClicked -> {
                    mutableState.update {
                        it.copy(
                            demoResult = DesignSystemDemoState.Error(UiText.StringResource(R.string.design_system_error_sample)),
                        )
                    }
                }
            }
        }
    }
