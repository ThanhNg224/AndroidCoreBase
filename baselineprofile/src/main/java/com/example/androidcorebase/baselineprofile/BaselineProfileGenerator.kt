package com.example.androidcorebase.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val APP_PACKAGE_NAME = "com.example.androidcorebase"

/**
 * Exercises [CriticalJourney] -- the same journey [StartupBenchmark] measures -- so the generated
 * `baseline-prof.txt` covers real code paths, not just process init.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() =
        baselineProfileRule.collect(packageName = APP_PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()
            CriticalJourney.execute(device, APP_PACKAGE_NAME)
        }
}
