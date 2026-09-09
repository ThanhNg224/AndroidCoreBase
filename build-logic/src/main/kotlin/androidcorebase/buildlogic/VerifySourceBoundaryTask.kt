package androidcorebase.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when any `.kt` file under [sourceRoot] imports a forbidden framework prefix.
 * [sourceRoot] is optional/skip-when-empty so a module without that source root (or before it
 * exists) reports NO-SOURCE instead of failing configuration or validation.
 */
abstract class VerifySourceBoundaryTask : DefaultTask() {

    @get:Optional
    @get:SkipWhenEmpty
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoot: DirectoryProperty

    @get:Input
    abstract val forbiddenImportPrefixes: ListProperty<String>

    @TaskAction
    fun verify() {
        val root = sourceRoot.get().asFile
        val prefixes = forbiddenImportPrefixes.get()
        val violations = mutableListOf<String>()

        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trimStart()
                    if (trimmed.startsWith("import ")) {
                        val importPath = trimmed.removePrefix("import ").trim()
                        val offending = prefixes.firstOrNull { importPath.startsWith(it) }
                        if (offending != null) {
                            violations +=
                                "${file.relativeTo(root)}:${index + 1}: forbidden import '$importPath' " +
                                    "(matches forbidden prefix '$offending')"
                        }
                    }
                }
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Framework-independent source boundary violated in ${root.path}:\n" +
                    violations.joinToString("\n"),
            )
        }
    }
}
