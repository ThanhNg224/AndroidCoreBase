package com.example.androidcorebase.sample.demo.presentation.state

sealed interface DemoUiEvent {
    data object IncrementClicked : DemoUiEvent

    data object RefreshWeatherClicked : DemoUiEvent
}
