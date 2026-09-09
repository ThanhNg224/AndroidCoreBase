package com.example.androidcorebase.baselineprofile

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

private const val WAIT_TIMEOUT_MS = 5_000L

/**
 * The one critical journey both [BaselineProfileGenerator] (profile collection) and
 * [StartupBenchmark] (measured None-vs-Partial comparison) exercise, so the profile actually
 * covers the code paths the benchmark measures. Every step waits for a resource this starter app
 * itself owns before moving on, rather than a fixed delay.
 */
internal object CriticalJourney {
    fun execute(
        device: UiDevice,
        packageName: String,
    ) {
        device.wait(Until.hasObject(By.res(packageName, "tvGreeting")), WAIT_TIMEOUT_MS)

        device.findObject(By.res(packageName, "demoFragment"))?.click()
        check(device.wait(Until.hasObject(By.res(packageName, "tvWeather")), WAIT_TIMEOUT_MS)) { "Demo tab never showed tvWeather" }
        device.findObject(By.res(packageName, "btnIncrement"))?.click()

        device.findObject(By.res(packageName, "designSystemFragment"))?.click()
        check(device.wait(Until.hasObject(By.res(packageName, "composeInteropDemo")), WAIT_TIMEOUT_MS)) {
            "UI Kit tab never showed composeInteropDemo"
        }

        device.findObject(By.res(packageName, "actionSettings"))?.click()
        check(device.wait(Until.hasObject(By.res(packageName, "rowLanguage")), WAIT_TIMEOUT_MS)) { "Settings never showed rowLanguage" }
        device.findObject(By.res(packageName, "rowAppearance"))?.click()
        device.wait(Until.hasObject(By.text("Dark")), WAIT_TIMEOUT_MS)
        device.findObject(By.text("Dark"))?.click()
        device.pressBack()

        device.findObject(By.res(packageName, "homeFragment"))?.click()
        check(device.wait(Until.hasObject(By.res(packageName, "tvGreeting")), WAIT_TIMEOUT_MS)) { "Never returned to Home" }
    }
}
