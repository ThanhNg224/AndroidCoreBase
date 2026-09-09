@file:Suppress("UnstableApiUsage")

plugins {
    id("androidcorebase.android-library")
    id("androidcorebase.quality")
    id("androidcorebase.published-library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.thanhng224.androidcorebase.core"
    resourcePrefix = "core_"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testFixtures {
        enable = true
    }
}

publishedLibrary {
    artifactId.set("AndroidCoreBase")
    displayName.set("AndroidCoreBase Core")
    description.set("Reusable XML + ViewBinding, MVVM + Clean Architecture Android base.")
}

kotlin {
    explicitApi()
}

dependencies {
    // AndroidX & Core UI
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    api(libs.androidx.fragment.ktx)
    api(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    api(libs.material)
    implementation(libs.lottie)

    // Coroutines
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Storage & Network
    api(libs.androidx.datastore.preferences)
    api(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Test & Test Fixtures
    testFixturesImplementation(libs.junit)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

kover {
    reports {
        filters {
            // Positive selection of the deterministic (non-UI, non-Android-glue) surface, rather
            // than a wildcard-plus-growing-exclusion-list: `core.ui.components`/`core.ui.window`
            // (real View/Window glue) and `core.ui.base`'s Activity/Fragment hosts are simply never
            // listed here at all, instead of being excluded from a catch-all `core.*` include.
            includes {
                classes(
                    "com.thanhng224.androidcorebase.core.foundation.*",
                    "com.thanhng224.androidcorebase.core.architecture.*",
                    "com.thanhng224.androidcorebase.core.localization.*",
                    "com.thanhng224.androidcorebase.core.network.*",
                    "com.thanhng224.androidcorebase.core.network.auth.*",
                    "com.thanhng224.androidcorebase.core.network.transfer.*",
                    "com.thanhng224.androidcorebase.core.storage.settings.*",
                    "com.thanhng224.androidcorebase.core.storage.secure.*",
                    "com.thanhng224.androidcorebase.core.ui.theme.*",
                    "com.thanhng224.androidcorebase.core.ui.text.*",
                    "com.thanhng224.androidcorebase.core.navigation.*",
                )
            }
            excludes {
                classes(
                    // Generated code
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    // Property-delegate helpers over android.os.Bundle/Intent -- framework glue, not
                    // business logic, even though the package above is otherwise deterministic.
                    "*.core.navigation.ArgumentDelegatesKt",
                    "*.core.navigation.IntentExtraDelegate",
                    "*.core.navigation.IntentExtraNullableDelegate",
                    "*.core.navigation.FragmentArgumentDelegate",
                    "*.core.navigation.FragmentArgumentNullableDelegate",
                    // Android System & Storage Services
                    "*.core.storage.secure.EncryptedFileSecureStore*",
                    "*.core.storage.secure.EncryptedFileCodec*",
                    "*.core.localization.AppCompatLocaleApplier*",
                )
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
