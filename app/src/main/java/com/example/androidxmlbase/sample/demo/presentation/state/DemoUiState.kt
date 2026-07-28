package com.example.androidxmlbase.sample.demo.presentation.state

import com.thanhng224.androidxmlbase.core.architecture.UiState

data class DemoUiState(
    val count: Int = 0,
    val weather: DemoWeatherState = DemoWeatherState.Loading,
) : UiState
