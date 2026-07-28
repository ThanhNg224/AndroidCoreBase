plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.androidxmlbase"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.androidxmlbase"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"https://api.open-meteo.com/\"")
        buildConfigField("boolean", "API_ENABLE_LOGGING", "false")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.intuit.sdp)
    implementation(libs.intuit.ssp)
    implementation(libs.hilt.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.shimmer)
    implementation(libs.lottie)
    implementation(libs.timber)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
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
                classes(
                    "*.sample.demo.data.mapper.*",
                    "*.sample.demo.domain.usecase.FetchDemoWeatherUseCase",
                    "*.sample.demo.domain.usecase.IncrementCounterUseCase",
                    "*.sample.demo.domain.usecase.ObserveDemoCountUseCase",
                    "*.sample.demo.domain.usecase.SaveDemoCountUseCase",
                    "*.sample.demo.presentation.viewmodel.DemoViewModel",
                    "*.sample.designsystem.presentation.viewmodel.DesignSystemViewModel",
                )
            }
            excludes {
                classes(
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
                    "*.AndroidXmlBaseApplication",
                    "*.MainActivity",
                    "*Activity",
                    "*Fragment",
                    "*DialogFragment",
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
