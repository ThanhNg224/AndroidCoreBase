package com.example.androidxmlbase.sample.demo.presentation.state

import com.thanhng224.androidxmlbase.core.architecture.UiEffect

sealed interface DemoUiEffect : UiEffect {
    data object ShowMaxCountReached : DemoUiEffect
}
