package androidcorebase.buildlogic

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

/**
 * Registers `apiDump`/`apiCheck` JavaExec tasks that run Metalava directly against this module's
 * public source API and wires `apiCheck` into `check`. There is no maintained Gradle plugin for
 * standalone Metalava use, so this drives the tool -- the one AndroidX itself uses -- imperatively
 * over this module's sources plus the Android boot classpath (AGP 9's
 * `androidComponents.sdkComponents`, since the old `android.bootClasspath` accessor is gone).
 *
 * [apiFileName] is committed at `api/<apiFileName>` and lets each published module keep its own
 * name (`core.api`, `ui-compose.api`) while sharing this implementation.
 */
fun Project.registerApiTasks(
    metalavaClasspath: Configuration,
    apiFileName: String,
) {
    val androidComponents = extensions.getByType<LibraryAndroidComponentsExtension>()
    val bootClasspathString =
        androidComponents.sdkComponents.bootClasspath
            .map { files -> files.joinToString(File.pathSeparator) { it.asFile.absolutePath } }

    val mainSourceDir = file("src/main/java")
    val committedApiFile = layout.projectDirectory.file("api/$apiFileName").asFile
    val generatedApiFile = layout.buildDirectory.file("metalava/$apiFileName").get().asFile

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
        description = "Regenerates api/$apiFileName from this module's public source API."
        val committed = committedApiFile
        configureMetalava(committed)
        doLast { committed.writeText(committed.readText().trimEnd() + "\n") }
    }

    val apiCheck =
        tasks.register<JavaExec>("apiCheck") {
            group = "verification"
            description = "Fails if api/$apiFileName is stale -- run apiDump and review the diff."
            configureMetalava(generatedApiFile)
            val committed = committedApiFile
            val generated = generatedApiFile
            doLast {
                if (!committed.exists()) {
                    throw GradleException("api/$apiFileName is missing. Run apiDump and commit it.")
                }
                val committedText = committed.readText().trimEnd() + "\n"
                val generatedText = generated.readText().trimEnd() + "\n"
                if (committed.readText() != committedText || committedText != generatedText) {
                    throw GradleException(
                        "Public API differs from the committed api/$apiFileName. " +
                            "Run apiDump, review the diff, and commit it if intended.",
                    )
                }
            }
        }

    tasks.named("check") { dependsOn(apiCheck) }
}
