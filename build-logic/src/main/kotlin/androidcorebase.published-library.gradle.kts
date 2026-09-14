import androidcorebase.buildlogic.registerApiTasks
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType

plugins {
    id("maven-publish")
}

/** Per-module publication metadata; set from the consuming module's own build file. */
interface PublishedLibraryExtension {
    val artifactId: Property<String>
    val displayName: Property<String>
    val description: Property<String>
}

val publishedLibrary = extensions.create<PublishedLibraryExtension>("publishedLibrary")

extensions.configure<LibraryExtension> {
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// A temporary Maven repository for `scripts/verify-publication.sh`; absent for a normal local
// build, where `publish` simply has nowhere configured to go besides mavenLocal.
val temporaryRepoUrl = providers.gradleProperty("publishRepoUrl")

publishing {
    if (temporaryRepoUrl.isPresent) {
        repositories {
            maven {
                name = "temporary"
                setUrl(temporaryRepoUrl.get())
            }
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.github.ThanhNg224"
                artifactId = publishedLibrary.artifactId.get()
                // VERSION_NAME lives in :core/gradle.properties. Resolve it explicitly for both
                // published modules instead of relying on Gradle's deprecated parent-project
                // property lookup from :core:ui-compose.
                val localVersionName = if (project.path == ":core") {
                    project.findProperty("VERSION_NAME") as? String
                } else {
                    rootProject.project(":core").findProperty("VERSION_NAME") as? String
                }
                version = System.getenv("VERSION")
                    ?: localVersionName
                    ?: error("VERSION_NAME is required to publish ${project.path}")

                from(components["release"])

                // AGP publishes test-fixtures capabilities as additional Maven variants. Maven
                // POM cannot represent those capabilities, while Gradle Module Metadata keeps
                // them intact and is the format used by Gradle 6+ consumers. The publication
                // gate verifies the resulting POM/AAR separately, so suppress only these known,
                // intentional metadata-loss warnings.
                suppressPomMetadataWarningsFor("releaseTestFixturesVariantReleaseApiPublication")
                suppressPomMetadataWarningsFor("releaseTestFixturesVariantReleaseRuntimePublication")

                pom {
                    name.set(publishedLibrary.displayName.get())
                    description.set(publishedLibrary.description.get())
                    url.set("https://github.com/ThanhNg224/AndroidCoreBase")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/ThanhNg224/AndroidCoreBase/blob/main/LICENSE")
                        }
                    }

                    // AGP folds this module's testFixtures dependencies (junit, kotlinx-coroutines-test)
                    // into the flattened Maven POM alongside the release variant's real dependencies, even
                    // though they're test-only and never reach a real consumer's compile/runtime classpath
                    // under Gradle (which resolves the separate, variant-aware Gradle Module Metadata
                    // instead). Strip them from the POM so POM-only tooling doesn't see them either.
                    withXml {
                        val testOnlyArtifactIds = setOf("junit", "kotlinx-coroutines-test")
                        val dependenciesNode =
                            asNode().children().filterIsInstance<groovy.util.Node>().find {
                                it.name().toString().endsWith("dependencies")
                            } ?: return@withXml
                        dependenciesNode.children().filterIsInstance<groovy.util.Node>().toList().forEach { dependencyNode ->
                            val artifactId =
                                dependencyNode.children().filterIsInstance<groovy.util.Node>().find {
                                    it.name().toString().endsWith("artifactId")
                                }?.text()
                            if (artifactId in testOnlyArtifactIds) {
                                dependenciesNode.remove(dependencyNode)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Precompiled script plugins don't get the generated `libs.*` accessors that a real project build
// script does, so the catalog is looked up imperatively here.
val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val metalavaClasspath: Configuration = configurations.create("metalavaClasspath")

dependencies {
    metalavaClasspath(libsCatalog.findLibrary("metalava").get())
}

registerApiTasks(metalavaClasspath, apiFileName = "${project.name}.api")
