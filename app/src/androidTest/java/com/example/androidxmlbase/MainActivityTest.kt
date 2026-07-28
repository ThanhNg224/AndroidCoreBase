package com.example.androidxmlbase

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun homeTab_isSelectedByDefault() {
        onView(withId(R.id.homeFragment)).check(matches(isSelected()))
        onView(withId(R.id.tvGreeting))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.appshell_home_greeting)))
    }

    @Test
    fun bottomNavigation_switchesBetweenTopLevelDestinations() {
        onView(withId(R.id.demoFragment)).perform(click())
        onView(withId(R.id.tvCount)).check(matches(isDisplayed()))

        onView(withId(R.id.designSystemFragment)).perform(click())
        onView(withText(R.string.design_system_headline_sample)).check(matches(isDisplayed()))

        onView(withId(R.id.demoFragment)).perform(click())
        onView(withId(R.id.tvCount)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsAction_opensSettingsScreen() {
        onView(withId(R.id.actionSettings)).perform(click())

        onView(withText(R.string.settings_personalization_section)).check(matches(isDisplayed()))
    }
}
