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

    // Dependency Injection
    api(libs.hilt.android)

    // Compose
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

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

// ---------------------------------------------------------------------------------------------
// Public API tracking (F1). binary-compatibility-validator cannot see Android library variants, so
// this drives metalava -- the tool AndroidX itself uses -- directly. There is no maintained Gradle
// plugin for standalone use (me.tylerbwong.gradle.metalava was last released in 2022), so the
// wiring is a plain JavaExec over this module's sources plus the Android boot classpath.
//
//   ./gradlew :core:apiDump     regenerate core/api/core.api
//   ./gradlew :core:apiCheck    fail if the committed dump is stale (wired into `check`)
// ---------------------------------------------------------------------------------------------
val metalavaClasspath: Configuration = configurations.create("metalavaClasspath")

dependencies {
    metalavaClasspath(libs.metalava)
}

// bootClasspath comes from androidComponents.sdkComponents in AGP 9 (the old
// `android.bootClasspath` accessor is gone). Kept as a Provider and read inside an argument
// provider so the configuration cache stays happy.
val bootClasspathString =
    androidComponents.sdkComponents.bootClasspath
        .map { files ->
            files.joinToString(File.pathSeparator) { it.asFile.absolutePath }
        }

val mainSourceDir = file("src/main/java")
val committedApiFile = layout.projectDirectory.file("api/core.api").asFile
val generatedApiFile =
    layout.buildDirectory
        .file("metalava/core.api")
        .get()
        .asFile

fun JavaExec.configureMetalava(output: File) {
    classpath = metalavaClasspath
    mainClass.set("com.android.tools.metalava.Driver")
    outputs.upToDateWhen { false }
    val sourcePath = mainSourceDir.absolutePath
    val outPath = output.absolutePath
    val bootCp = bootClasspathString
    doFirst { output.parentFile.mkdirs() }
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                "main",
                "--source-path",
                sourcePath,
                "--classpath",
                bootCp.get(),
                "--api",
                outPath,
                "--format",
                "4.0",
            )
        },
    )
}

tasks.register<JavaExec>("apiDump") {
    group = "verification"
    description = "Regenerates core/api/core.api from :core's public source API."
    configureMetalava(committedApiFile)
}

val apiCheck =
    tasks.register<JavaExec>("apiCheck") {
        group = "verification"
        description = "Fails if core/api/core.api is stale -- run :core:apiDump and review the diff."
        configureMetalava(generatedApiFile)
        // Copied into locals so the doLast action captures plain Files rather than a reference to
        // this build script, which the configuration cache cannot serialize.
        val committed = committedApiFile
        val generated = generatedApiFile
        doLast {
            if (!committed.exists()) {
                throw GradleException("core/api/core.api is missing. Run ./gradlew :core:apiDump and commit it.")
            }
            if (committed.readText() != generated.readText()) {
                throw GradleException(
                    ":core's public API differs from the committed core/api/core.api. " +
                        "Run ./gradlew :core:apiDump, review the diff, and commit it if intended.",
                )
            }
        }
    }

tasks.named("check") { dependsOn(apiCheck) }
