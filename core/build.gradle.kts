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

        buildConfigField("String", "API_BASE_URL", "\"https://api.open-meteo.com/\"")
        buildConfigField("boolean", "API_ENABLE_LOGGING", "false")
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

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.ThanhNg224"
            artifactId = "AndroidXmlBase"
            version = System.getenv("VERSION") ?: project.property("VERSION_NAME") as String

            afterEvaluate {
                from(components["release"])
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
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    implementation(libs.material)
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
                classes(
                    "*.core.architecture.result.ResultState*",
                    "*.core.architecture.result.DomainResult*",
                    "*.core.architecture.StateViewModel*",
                    "*.core.architecture.DefaultAppDispatchers",
                    "*.core.localization.LocaleManager*",
                    "*.core.navigation.NavigationOptions*",
                    "*.core.network.ApiResult*",
                    "*.core.network.RetrofitApiClient*",
                    "*.core.network.auth.SecureStoreAuthTokenProvider*",
                    "*.core.network.transfer.TransferResult*",
                    "*.core.network.transfer.ProgressRequestBody*",
                    "*.core.network.transfer.OkHttpFileTransferClient*",
                    "*.core.network.auth.*",
                    "*.core.network.connectivity.*",
                    "*.core.network.transfer.*",
                    "*.core.storage.settings.DataStoreSettingsStore*",
                    "*.core.storage.settings.SettingsKey*",
                    "*.core.storage.database.DbPassphraseProvider*",
                    "*.core.logging.ReleaseTree*",
                    "*.core.ui.base.Debouncer*",
                    "*.core.ui.base.ResultRenderState*",
                    "*.core.ui.drawable.ShapeDrawableFactory*",
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
                    "*Activity",
                    "*Fragment",
                    "*DialogFragment",
                    "*.core.ui.components.*",
                    "*.core.work.HeartbeatWorker",
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
