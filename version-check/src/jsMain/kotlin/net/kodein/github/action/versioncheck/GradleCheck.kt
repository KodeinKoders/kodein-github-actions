package net.kodein.github.action.versioncheck

import actions.exec.ExecOptions
import actions.exec.exec


private const val TASK = "kodein_initScript_checkVersion"

internal const val INIT_SCRIPT = "$TASK.init.gradle.kts"

/** Registers a [TASK] that fails if a project of the build, the root one included, has not the `expectedVersion` property as version. */
internal val INIT_SCRIPT_CONTENT = """
    rootProject {
        tasks.register("$TASK") {
            doLast {
                val expectedVersion = project.providers.gradleProperty("expectedVersion").get()
                if (project.version != expectedVersion) throw Exception("Root project has version '${'$'}{project.version}', expected '${'$'}expectedVersion'")
                allprojects {
                    if (this.version != expectedVersion) throw Exception("Project ${'$'}{this.name} has version '${'$'}{this.version}', root project has version '${'$'}expectedVersion'")
                }
            }
        }
    }
""".trimIndent() + "\n"

/**
 * The arguments of `./gradlew`, whose wrapper is at the workspace's root.
 *
 * `-p` is only given when the build is not at the workspace's root. The init script is written in the build's path,
 * which `-p` already designates, so `-I` only names the file.
 */
internal fun gradleArguments(gradleBuildPath: String, expectedVersion: String): List<String> = buildList {
    add("--no-configuration-cache")
    add("--no-daemon")
    if (!isWorkspaceRoot(gradleBuildPath)) {
        add("-p")
        add(gradleBuildPath)
    }
    add("-I")
    add(INIT_SCRIPT)
    add("-PexpectedVersion=$expectedVersion")
    add(TASK)
    add("-q")
}

/**
 * Runs [TASK] on the Gradle build at [gradleBuildPath], which checks the version of every project of the build.
 *
 * The init script is written in the build's path for the time of the run.
 */
internal suspend fun checkGradleVersions(gradleBuildPath: String, expectedVersion: String): List<Problem> {
    val script = initScriptPath(gradleBuildPath)
    writeText(script, INIT_SCRIPT_CONTENT)
    val exitCode = try {
        // Gradle's own output is the explanation: it names the project that has the wrong version.
        exec("./gradlew", gradleArguments(gradleBuildPath, expectedVersion).toTypedArray(), ExecOptions(ignoreReturnCode = true)).toInt()
    } finally {
        delete(script)
    }
    return if (exitCode == 0) emptyList()
    else listOf(Problem("Not all Gradle projects have version \"$expectedVersion\": Gradle exited with $exitCode, see its output."))
}
