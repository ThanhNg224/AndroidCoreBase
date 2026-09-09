package com.example.androidcorebase.sample.demo.presentation.state

import com.thanhng224.androidcorebase.core.ui.text.UiText

data class PendingDemoMessage(
    val id: Long,
    val text: UiText,
    val actionLabel: UiText? = null,
    val action: DemoMessageAction? = null,
)

sealed interface DemoMessageAction {
    data object ResetCounter : DemoMessageAction
}
