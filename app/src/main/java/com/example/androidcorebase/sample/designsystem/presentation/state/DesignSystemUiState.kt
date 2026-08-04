package com.example.androidcorebase.sample.designsystem.presentation.state

import com.thanhng224.androidcorebase.core.architecture.UiState
import com.thanhng224.androidcorebase.core.architecture.result.ResultState

data class DesignSystemUiState(
    val demoResult: ResultState<Unit> = ResultState.Loading,
) : UiState
