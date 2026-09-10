package com.example.androidcorebase.baselineprofile

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

// Profile collection runs against a non-minified, uncompiled build, and CI collects on an
// emulator that is slower again than a physical device. Waits return as soon as the node appears,
// so a generous ceiling costs nothing on fast hardware and prevents flakes on slow hardware.
private const val WAIT_TIMEOUT_MS = 15_000L

/** Keeps the scroll gesture clear of the system back-gesture edges. */
private const val GESTURE_MARGIN_DIVISOR = 5

/** Index of DARK in SettingsFragment's `listOf(SYSTEM, LIGHT, DARK)` appearance dialog. */
private const val DARK_THEME_OPTION_INDEX = 2

/**
 * The one critical journey both [BaselineProfileGenerator] (profile collection) and
 * [StartupBenchmark] (measured None-vs-Partial comparison) exercise, so the profile actually
 * covers the code paths the benchmark measures. Every step waits for a resource this starter app
 * itself owns before moving on, rather than a fixed delay.
 */
internal object CriticalJourney {
    /**
     * `findObject(...)?.click()` silently does nothing when the node is missing, which then
     * surfaces as a misleading failure on the *next* `check`. Fail at the actual missing step.
     */
    private fun UiDevice.clickOrFail(
        selector: BySelector,
        what: String,
    ) {
        val node = checkNotNull(findObject(selector)) { "Could not find $what" }
        node.click()
    }

    fun execute(
        device: UiDevice,
        packageName: String,
    ) {
        device.wait(Until.hasObject(By.res(packageName, "tvGreeting")), WAIT_TIMEOUT_MS)

        device.clickOrFail(By.res(packageName, "demoFragment"), "the Demo tab")
        check(device.wait(Until.hasObject(By.res(packageName, "tvWeather")), WAIT_TIMEOUT_MS)) { "Demo tab never showed tvWeather" }
        device.clickOrFail(By.res(packageName, "btnIncrement"), "the increment button")

        device.clickOrFail(By.res(packageName, "designSystemFragment"), "the UI Kit tab")
        check(device.wait(Until.hasObject(By.res(packageName, "btnShowSnackbar")), WAIT_TIMEOUT_MS)) {
            "UI Kit tab never rendered"
        }
        // fragment_design_system.xml is a tall ScrollView whose last child is the Compose interop
        // ComposeView. UiAutomator only sees nodes in the accessibility tree, so that view does not
        // exist for By.res until it is scrolled into range.
        val scroller = checkNotNull(device.findObject(By.scrollable(true))) { "UI Kit tab is not scrollable" }
        scroller.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
        scroller.scrollUntil(Direction.DOWN, Until.scrollFinished(Direction.DOWN))
        check(device.wait(Until.hasObject(By.res(packageName, "composeInteropDemo")), WAIT_TIMEOUT_MS)) {
            "UI Kit tab never showed composeInteropDemo after scrolling to the bottom"
        }

        device.clickOrFail(By.res(packageName, "actionSettings"), "the Settings menu item")
        check(device.wait(Until.hasObject(By.res(packageName, "rowLanguage")), WAIT_TIMEOUT_MS)) { "Settings never showed rowLanguage" }
        device.clickOrFail(By.res(packageName, "rowAppearance"), "the Appearance row")
        // Pick the option by position, not by label. The dialog renders localized strings
        // (values-vi turns "Dark" into "Tối"), so matching text couples this journey to whichever
        // locale the device or a previous test left the app in.
        val themeItems = By.res("android", "text1")
        check(device.wait(Until.hasObject(themeItems), WAIT_TIMEOUT_MS)) { "Appearance dialog never opened" }
        val options = device.findObjects(themeItems)
        check(options.size > DARK_THEME_OPTION_INDEX) {
            "Appearance dialog showed ${options.size} options, expected at least ${DARK_THEME_OPTION_INDEX + 1}"
        }
        options[DARK_THEME_OPTION_INDEX].click()
        device.pressBack()

        device.clickOrFail(By.res(packageName, "homeFragment"), "the Home tab")
        check(device.wait(Until.hasObject(By.res(packageName, "tvGreeting")), WAIT_TIMEOUT_MS)) { "Never returned to Home" }
    }
}
