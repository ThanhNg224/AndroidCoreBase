@file:Suppress("UnstableApiUsage")

plugins {
    id("androidcorebase.android-library")
    id("androidcorebase.quality")
    id("androidcorebase.published-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    api(libs.material)
    implementation(libs.lottie)
    api(libs.timber)

    // Coroutines
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Storage & Network
    implementation(libs.androidx.datastore.preferences)
    api(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Dependency Injection
    api(libs.hilt.android)

    // KSP Annotation Processors
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Test & Test Fixtures
    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.work.testing)
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
                    "*_Factory*",
                    "*_HiltModules*",
                    "*_MembersInjector*",
                    "*Hilt_*",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                    // Dependency Injection
                    "*.core.di.*",
                    "*.core.ui.theme.ThemeModule*",
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
                    "*.core.ui.base.ResultStateOverlayKt",
                    "*.core.ui.base.DebouncerKt",
                    "*.core.ui.components.*",
                    "*.core.ui.window.*",
                    // Android System & Storage Services
                    "*.core.startup.*",
                    "*.core.storage.secure.EncryptedSecureStore*",
                    "*.core.storage.settings.AppDataStoreKt",
                    "*.core.localization.AppCompatLocaleApplier*",
                    "*.core.localization.LocaleAppContext*",
                    "*.core.navigation.ActivityNavigator*",
                    "*.core.network.connectivity.AndroidConnectivityChecker*",
                    "*.core.time.AndroidElapsedRealtimeClock*",
                    "*.core.ui.text.AndroidStringProvider*",
                    "*.core.work.*",
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
