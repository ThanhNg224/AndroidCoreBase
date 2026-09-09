package com.example.androidcorebase.sample.designsystem.presentation.state

import com.thanhng224.androidcorebase.core.ui.text.UiText

sealed interface DesignSystemDemoState {
    data object Loading : DesignSystemDemoState

    data object Success : DesignSystemDemoState

    data class Error(
        val message: UiText,
    ) : DesignSystemDemoState
}
