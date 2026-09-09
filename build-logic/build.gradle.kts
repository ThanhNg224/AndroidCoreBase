plugins {
    `kotlin-dsl`
}

// Compose compiler is applied directly by consuming modules via their own `plugins {}` alias, not
// by a convention plugin here, so it is deliberately absent from this classpath: an unused
// `implementation` dependency here would put its classes on every project's shared plugin
// classloader and break the consuming module's own explicit, versioned plugin request.
dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
}
