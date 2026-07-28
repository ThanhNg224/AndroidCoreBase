package com.example.androidxmlbase.feature.settings.presentation.ui

import android.os.Bundle
import com.example.androidxmlbase.feature.settings.domain.usecase.SetLanguageUseCase
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import com.thanhng224.androidxmlbase.core.navigation.getTyped
import com.thanhng224.androidxmlbase.core.ui.transition.TransitionAction
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
