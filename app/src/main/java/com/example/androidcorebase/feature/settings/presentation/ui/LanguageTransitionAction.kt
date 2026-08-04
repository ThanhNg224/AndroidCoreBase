package com.example.androidcorebase.feature.settings.presentation.ui

import android.os.Bundle
import com.example.androidcorebase.feature.settings.domain.usecase.SetLanguageUseCase
import com.thanhng224.androidcorebase.core.localization.AppLanguage
import com.thanhng224.androidcorebase.core.navigation.getTyped
import com.thanhng224.androidcorebase.core.ui.transition.TransitionAction
import javax.inject.Inject

class LanguageTransitionAction
    @Inject
    constructor(
        private val setLanguage: SetLanguageUseCase,
    ) : TransitionAction {
        override suspend fun perform(extras: Bundle) {
            val tag = extras.getTyped(EXTRA_LANGUAGE_TAG, String::class.java)
            setLanguage(AppLanguage.findByLanguageTag(tag.orEmpty()))
        }

        companion object {
            const val KEY = "settings_language"
            const val EXTRA_LANGUAGE_TAG = "extra_language_tag"
        }
    }
