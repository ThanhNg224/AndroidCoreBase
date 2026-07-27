package com.example.androidxmlbase.feature.settings.presentation.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidxmlbase.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.thanhng224.androidxmlbase.core.R as CoreR

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(SettingsActivity::class.java)

    @Test
    fun themeRow_opensSingleChoiceDialogOnTheSameScreen() {
        onView(withId(R.id.rowAppearance)).perform(click())

        onView(withText(R.string.settings_appearance_dialog_title)).check(matches(isDisplayed()))
        onView(withText(R.string.settings_theme_system)).check(matches(isDisplayed()))
        onView(withText(R.string.settings_theme_light)).check(matches(isDisplayed()))
        onView(withText(R.string.settings_theme_dark)).check(matches(isDisplayed()))
    }

    @Test
    fun languageRow_opensSingleChoiceDialogOnTheSameScreen() {
        onView(withId(R.id.rowLanguage)).perform(click())

        onView(withText(R.string.settings_language_dialog_title)).check(matches(isDisplayed()))
        onView(withText(R.string.settings_language_system)).check(matches(isDisplayed()))
        onView(withText(CoreR.string.core_language_english)).check(matches(isDisplayed()))
        onView(withText(CoreR.string.core_language_vietnamese)).check(matches(isDisplayed()))
    }
}
