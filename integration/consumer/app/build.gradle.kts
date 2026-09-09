plugins {
    id("com.android.application")
}

// Toggles whether this consumer build resolves and exercises the optional
// `AndroidCoreBase-ui-compose` artifact, per -PincludeCompose=true|false (default false).
val includeCompose = providers.gradleProperty("includeCompose").map(String::toBoolean).getOrElse(false)

if (includeCompose) {
    apply(plugin = "org.jetbrains.kotlin.plugin.compose")
}

val androidCoreBaseVersion =
    providers.gradleProperty("androidCoreBaseVersion").orNull
        ?: error("-PandroidCoreBaseVersion=<version> is required")

android {
    namespace = "consumer.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "consumer.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        compose = includeCompose
    }

    // This module only ever builds one mode per invocation (see scripts/verify-publication.sh,
    // which runs `:app:assemble*` once per -PincludeCompose value), so the compose-only source set
    // is added to `main` directly rather than modeled as a product flavor.
    sourceSets {
        getByName("main") {
            kotlin.srcDir(if (includeCompose) "src/compose/java" else "src/standalone/java")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.github.ThanhNg224:AndroidCoreBase:$androidCoreBaseVersion")
    if (includeCompose) {
        implementation("com.github.ThanhNg224:AndroidCoreBase-ui-compose:$androidCoreBaseVersion")
    }
}
