plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.baselineprofile)
    // Required because :app writes @Composable code (DesignSystemFragment's ComposeInteropDemo
    // and the lambda it passes into :core's ComposeView.setThemedContent). Without this plugin,
    // this module's compiler treats @Composable () -> Unit as a plain Function0 instead of doing
    // the composer-parameter ABI transform, so a call site here would produce a Function0 call
    // against a callee that :core (which does have the plugin) compiled as Function2 -- a
    // NoSuchMethodError at runtime that compiles cleanly, because Kotlin's type checker sees the
    // same declared type on both sides and only the bytecode shape actually differs.
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.androidcorebase"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.androidcorebase"
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// F16 workaround. Minifying activates the Compose compiler plugin's produceRelease/BenchmarkComposeMapping
// tasks, which resolve org.jetbrains.kotlin:compose-group-mapping at a version the plugin hardcodes --
// 2.2.10 as of plugin 2.4.10. That artifact only exists from 2.3.0-Beta1 onward, so the request can
// never resolve and the build fails at configuration time. Forcing it to our Kotlin version works
// because a matching artifact is published (verified: compose-group-mapping:2.4.10 is on Maven
// Central). Remove this once the plugin stops hardcoding it -- see docs/MODERNIZATION.md F16.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "compose-group-mapping") {
            useVersion(libs.versions.kotlin.get())
            because("plugin hardcodes a nonexistent version; see F16")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(testFixtures(project(":core")))
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
    implementation(libs.hilt.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.shimmer)
    implementation(libs.lottie)
    implementation(libs.timber)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.work.testing)
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
                    "*.AndroidCoreBaseApplication",
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
