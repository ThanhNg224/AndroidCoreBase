package com.example.androidcorebase.sample.demo.presentation.state

import com.thanhng224.androidcorebase.core.architecture.UiState

data class DemoUiState(
    val count: Int = 0,
    val weather: DemoWeatherState = DemoWeatherState.Loading,
) : UiState
