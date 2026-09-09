pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Isolated on purpose: no mavenLocal(), no project substitution for AndroidCoreBase. The only
    // way this build can see the library under test is through the temporary repository that
    // scripts/verify-publication.sh publishes it to and passes in via -PpublishRepoUrl.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val publishRepoUrl =
            providers.gradleProperty("publishRepoUrl").orNull
                ?: error("-PpublishRepoUrl=<file-or-http-url> is required to resolve com.github.ThanhNg224:AndroidCoreBase*")
        maven {
            name = "temporary"
            url = uri(publishRepoUrl)
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "androidcorebase-consumer"
include(":app")
