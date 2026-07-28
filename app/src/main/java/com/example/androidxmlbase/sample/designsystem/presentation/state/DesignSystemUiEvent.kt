package com.example.androidxmlbase.sample.designsystem.presentation.state

import com.thanhng224.androidxmlbase.core.architecture.UiEvent

sealed interface DesignSystemUiEvent : UiEvent {
    data object ShowLoadingClicked : DesignSystemUiEvent

    data object ShowSuccessClicked : DesignSystemUiEvent

    data object ShowErrorClicked : DesignSystemUiEvent
}
