@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

android {
    namespace = "com.thanhng224.androidcorebase.core"
    resourcePrefix = "core_"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    testFixtures {
        enable = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warning += "ResourceName"
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.ThanhNg224"
            artifactId = "AndroidCoreBase"
            version = System.getenv("VERSION") ?: project.property("VERSION_NAME") as String

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("AndroidCoreBase Core")
                description.set("Reusable XML + ViewBinding, MVVM + Clean Architecture Android base.")
                url.set("https://github.com/ThanhNg224/AndroidCoreBase")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/ThanhNg224/AndroidCoreBase/blob/main/LICENSE")
                    }
                }
            }
        }
    }
}

kotlin {
    explicitApi()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.sqlcipher.android)

    // Dependency Injection
    api(libs.hilt.android)

    // Compose
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    // KSP Annotation Processors
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
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

configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

ktlint {
    android = true
    outputToConsole = true
    filter {
        exclude("**/generated/**")
    }
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
                    "*.core.ui.theme.ComposeThemeKt",
                    "*.core.ui.base.ComposeInteropKt",
                    // Android System & Storage Services
                    "*.core.startup.*",
                    "*.core.storage.database.AppDatabase*",
                    "*.core.storage.database.LocalSettingDao*",
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
