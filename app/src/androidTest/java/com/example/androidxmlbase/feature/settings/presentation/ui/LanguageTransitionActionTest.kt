package com.example.androidxmlbase.feature.settings.presentation.ui

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanhng224.androidxmlbase.core.localization.AppLanguage
import com.thanhng224.androidxmlbase.core.localization.LocaleManager
import com.thanhng224.androidxmlbase.core.ui.base.TransitionActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageTransitionActionTest {
    private val localeManager = LocaleManager()

    @get:Rule
    val activityRule =
        ActivityScenarioRule<TransitionActivity>(
            TransitionActivity.createIntent(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                actionKey = LanguageTransitionAction.KEY,
                extras =
                    Bundle().apply {
                        putString(LanguageTransitionAction.EXTRA_LANGUAGE_TAG, AppLanguage.VIETNAMESE.languageTag)
                    },
            ),
        )

    @Before
    fun setUp() {
        localeManager.useSystemLanguage()
    }

    @After
    fun tearDown() {
        localeManager.useSystemLanguage()
    }

    @Test
    fun transitionActivity_runsActionThenFinishes() {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (activityRule.scenario.state != Lifecycle.State.DESTROYED && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS)
        }

        assertEquals(Lifecycle.State.DESTROYED, activityRule.scenario.state)
        assertEquals(AppLanguage.VIETNAMESE, localeManager.currentLanguage())
    }

    private companion object {
        const val TIMEOUT_MS = 3_000L
        const val POLL_INTERVAL_MS = 50L
    }
}
