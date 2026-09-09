import androidcorebase.buildlogic.VerifySourceBoundaryTask

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
}

ktlint {
    android = true
    outputToConsole = true
    filter {
        exclude("**/generated/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

// Coverage bounds are a per-module decision, not a shared convention: :core keeps its own filtered
// `verify` rule and :core:ui-compose declares none, since it is lifecycle/UI glue with no
// meaningful unit-test surface. A shared `minBound` here would fail its `check` task.
//
// `verifyDeterministicCoreCoverage` is a stable, memorable alias for Kover's own `koverVerify`
// task, shared by every module using this convention so a module with no `verify {}` rule (like
// :core:ui-compose) still exposes the same task name, vacuously passing.
val verifyDeterministicCoreCoverage =
    tasks.register("verifyDeterministicCoreCoverage") {
        group = "verification"
        description = "Alias for koverVerify: enforces this module's deterministic (non-UI) Kover coverage rule, if any."
        dependsOn("koverVerify")
    }
tasks.named("check") { dependsOn(verifyDeterministicCoreCoverage) }

val verifyFrameworkIndependentSources =
    tasks.register<VerifySourceBoundaryTask>("verifyFrameworkIndependentSources") {
        group = "verification"
        description = "Fails when core.foundation imports a framework/transport type."
        sourceRoot.set(
            layout.projectDirectory.dir("src/main/java/com/thanhng224/androidcorebase/core/foundation"),
        )
        forbiddenImportPrefixes.set(
            listOf(
                "android.",
                "androidx.",
                "retrofit2.",
                "okhttp3.",
                "dagger.",
                "javax.inject.",
                "com.google.android.material.",
                "com.thanhng224.androidcorebase.core.R",
            ),
        )
    }

tasks.named("check") { dependsOn(verifyFrameworkIndependentSources) }
