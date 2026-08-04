package com.example.androidcorebase.sample.demo.presentation.state

import com.thanhng224.androidcorebase.core.architecture.UiEffect

sealed interface DemoUiEffect : UiEffect {
    data object ShowMaxCountReached : DemoUiEffect
}
