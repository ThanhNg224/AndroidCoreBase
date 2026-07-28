plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    `maven-publish`
}

android {
    namespace = "com.thanhng224.androidxmlbase.core"
    resourcePrefix = "core_"
    compileSdk {
        version = release(37)
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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
    }
    // Publishes the fakes in src/testFixtures so consuming apps can test against :core's
    // contracts without hand-rolling doubles: testImplementation(testFixtures("...:AndroidXmlBase:<v>"))
    testFixtures {
        enable = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // Files, ids and styleables carry the core_ prefix. Styles/themes instead follow the
        // platform's Type.Namespace.Variant convention (TextAppearance.AndroidXmlBase.Body, like
        // TextAppearance.MaterialComponents.Body1) — already namespaced, and core_-prefixing them
        // would be non-idiomatic. Kept visible as a warning rather than silenced.
        warning += "ResourceName"
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.ThanhNg224"
            artifactId = "AndroidXmlBase"
            version = System.getenv("VERSION") ?: project.property("VERSION_NAME") as String

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("AndroidXmlBase Core")
                description.set("Reusable XML + ViewBinding, MVVM + Clean Architecture Android base.")
                url.set("https://github.com/ThanhNg224/AndroidXmlBase")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/ThanhNg224/AndroidXmlBase/blob/main/LICENSE")
                    }
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
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
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    api(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.intuit.sdp)
    implementation(libs.intuit.ssp)
    api(libs.hilt.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.sqlcipher.android)
    implementation(libs.lottie)
    api(libs.timber)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    ksp(libs.androidx.hilt.compiler)
    testFixturesImplementation(libs.junit)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
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
                // Whole module, not a hand-picked allowlist. Anything genuinely untestable on the
                // JVM is excluded below with a reason.
                classes("com.thanhng224.androidxmlbase.core.*")
            }
            excludes {
                classes(
                    // Generated
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
                    // DI wiring: declarations only, exercised by Hilt's own codegen
                    "*.core.di.*",
                    "*.core.ui.theme.ThemeModule*",
                    // Android-framework glue: covered by androidTest, not JVM unit tests
                    "*Activity",
                    "*Activity$*",
                    "*Fragment",
                    "*Fragment$*",
                    "*DialogFragment",
                    // Need a real Bundle / FragmentManager / View: instrumented-only
                    "*.core.navigation.ArgumentDelegatesKt",
                    "*.core.navigation.IntentExtraDelegate",
                    "*.core.navigation.IntentExtraNullableDelegate",
                    "*.core.navigation.FragmentArgumentDelegate",
                    "*.core.navigation.FragmentArgumentNullableDelegate",
                    "*.core.ui.base.ResultStateOverlayKt",
                    "*.core.ui.base.DebouncerKt",
                    "*.core.ui.components.*",
                    "*.core.ui.responsive.*",
                    "*.core.ui.window.*",
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
                    // Reference implementation, intentionally unscheduled
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
