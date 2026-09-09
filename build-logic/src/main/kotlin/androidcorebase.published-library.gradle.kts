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
                version = System.getenv("VERSION") ?: project.property("VERSION_NAME") as String

                from(components["release"])

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
