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
            includes {
                classes("com.thanhng224.androidcorebase.core.*")
            }
            excludes {
                classes(
                    // Generated code
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    "*.databinding.*",
                    // Android UI & Components
                    "*Activity",
                    "*Activity$*",
                    "*Fragment",
                    "*Fragment$*",
                    "*DialogFragment",
                    "*.core.navigation.ArgumentDelegatesKt",
                    "*.core.navigation.IntentExtraDelegate",
                    "*.core.navigation.IntentExtraNullableDelegate",
                    "*.core.navigation.FragmentArgumentDelegate",
                    "*.core.navigation.FragmentArgumentNullableDelegate",
                    "*.core.ui.base.DebouncerKt",
                    "*.core.ui.components.*",
                    "*.core.ui.window.*",
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
