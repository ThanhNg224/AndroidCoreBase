plugins {
    // AGP 9's built-in Kotlin support means no separate org.jetbrains.kotlin.android plugin is needed.
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
