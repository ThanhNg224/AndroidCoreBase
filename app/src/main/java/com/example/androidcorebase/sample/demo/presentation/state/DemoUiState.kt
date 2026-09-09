package com.example.androidcorebase.sample.demo.presentation.state

data class DemoUiState(
    val count: Int = 0,
    val weather: DemoWeatherState = DemoWeatherState.Loading,
    val pendingMessages: List<PendingDemoMessage> = emptyList(),
)
