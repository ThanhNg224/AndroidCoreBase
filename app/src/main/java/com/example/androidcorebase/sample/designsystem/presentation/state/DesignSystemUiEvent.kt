package com.example.androidcorebase.sample.designsystem.presentation.state

import com.thanhng224.androidcorebase.core.architecture.UiEvent

sealed interface DesignSystemUiEvent : UiEvent {
    data object ShowLoadingClicked : DesignSystemUiEvent

    data object ShowSuccessClicked : DesignSystemUiEvent

    data object ShowErrorClicked : DesignSystemUiEvent
}
