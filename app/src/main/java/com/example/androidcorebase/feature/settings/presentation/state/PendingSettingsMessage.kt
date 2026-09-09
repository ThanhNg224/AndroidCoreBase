package com.example.androidcorebase.feature.settings.presentation.state

import com.thanhng224.androidcorebase.core.ui.text.UiText

data class PendingSettingsMessage(
    val id: Long,
    val text: UiText,
)
