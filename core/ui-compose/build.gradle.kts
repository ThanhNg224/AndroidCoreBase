plugins {
    id("androidcorebase.android-library")
    id("androidcorebase.quality")
    id("androidcorebase.published-library")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.thanhng224.androidcorebase.core.compose"

    buildFeatures {
        compose = true
    }
}

publishedLibrary {
    artifactId.set("AndroidCoreBase-ui-compose")
    displayName.set("AndroidCoreBase Compose Interop")
    description.set("Optional Jetpack Compose interoperability for AndroidCoreBase.")
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":core"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
}

// The spec exempts this module from a line-coverage percentage gate: it is 89 lines of
// lifecycle/Compose glue with no meaningful unit-test surface, covered instead by Lint, API
// checks, compilation, and the temporary-repository consumer build. No `kover { ... verify }`
// rule is declared here.
