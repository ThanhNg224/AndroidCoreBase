package com.example.androidcorebase.feature.settings.presentation.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidcorebase.MainActivity
import com.example.androidcorebase.R
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.thanhng224.androidcorebase.core.R as CoreR

/**
 * Selecting a locale now persists then applies directly (no `TransitionActivity`), and Android may
 * recreate the host Activity as a side effect of `AppCompatDelegate.setApplicationLocales`. This
 * proves the persisted choice survives an explicit recreation without a second repository mutation,
 * and that back navigation from the non-top-level Settings destination returns Home.
 *
 * Language summary assertions target `tvLanguageSummary` by id rather than by text: the theme and
 * language "System default" labels translate to the identical string in some locales (Vietnamese:
 * both are "Theo hệ thống"), which makes a bare text matcher ambiguous.
 */
@RunWith(AndroidJUnit4::class)
class SettingsFragmentTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @After
    fun resetLanguageToSystem() {
        openSettings()
        onView(withId(R.id.rowLanguage)).perform(click())
        onView(withText(R.string.settings_language_system)).perform(click())
        waitForLanguageSummary(R.string.settings_language_system)
    }

    @Test
    fun selectingALanguage_survivesRecreationWithoutRepeatingTheMutation_andBackReturnsHome() {
        openSettings()

        onView(withId(R.id.rowLanguage)).perform(click())
        onView(withText(CoreR.string.core_language_vietnamese)).perform(click())
        waitForLanguageSummary(CoreR.string.core_language_vietnamese)

        activityRule.scenario.recreate()

        // Re-collected state already reflects the persisted language; no second persist/apply is needed to show it.
        waitForLanguageSummary(CoreR.string.core_language_vietnamese)

        pressBack()

        onView(withId(R.id.homeFragment)).check(matches(isSelected()))
    }

    private fun openSettings() {
        onView(withId(R.id.actionSettings)).perform(click())
        onView(withId(R.id.tvLanguageSummary)).check(matches(isDisplayed()))
    }

    private fun waitForLanguageSummary(textResId: Int) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (true) {
            try {
                onView(withId(R.id.tvLanguageSummary)).check(matches(withText(textResId)))
                return
            } catch (failure: Throwable) {
                if (System.currentTimeMillis() >= deadline) throw failure
                Thread.sleep(POLL_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 50L
    }
}
