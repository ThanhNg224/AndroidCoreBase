plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.compiler.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
}
