package com.example.androidxmlbase.sample.demo.presentation.state

import com.thanhng224.androidxmlbase.core.architecture.UiEvent

sealed interface DemoUiEvent : UiEvent {
    data object IncrementClicked : DemoUiEvent

    data object RefreshWeatherClicked : DemoUiEvent
}
