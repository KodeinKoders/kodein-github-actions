package net.kodein.github.action.versioncheck

import node.fs.ReaddirSyncWithFileTypesOptions
import node.fs.existsSync
import node.fs.readFileSync
import node.fs.readdirSync


// All paths are relative to the working directory, which is the workspace once the repository is checked out.

internal fun exists(path: String): Boolean = existsSync(path)

internal fun readText(path: String): String = readFileSync(path, "utf8".unsafeCast<node.buffer.BufferEncoding>())

// Directories that hold other projects' files, or build output - never the repository's own build scripts.
private fun isSkipped(directory: String) = directory == "build" || directory == "node_modules" || directory.startsWith(".")

/** The path of [file] in the Gradle build that lives in [directory], relative to the workspace. */
private fun pathIn(directory: String, file: String): String {
    val trimmed = directory.trim().removePrefix("./").trimEnd('/')
    return if (trimmed.isEmpty() || trimmed == ".") file else "$trimmed/$file"
}

internal fun rootBuildPath(directory: String): String = pathIn(directory, ROOT_BUILD)

internal fun versionCatalogPath(directory: String): String = pathIn(directory, VERSION_CATALOG)

/** Every `build.gradle.kts` in the directory of [rootBuild] and below, except [rootBuild] itself. */
internal fun findOtherBuildScripts(rootBuild: String): List<String> =
    findBuildScripts(rootBuild.removeSuffix(ROOT_BUILD).trimEnd('/').ifEmpty { "." }).filter { it != rootBuild }

private fun findBuildScripts(directory: String): List<String> =
    readdirSync(directory, ReaddirSyncWithFileTypesOptions(withFileTypes = true)).flatMap { entry ->
        // Entries are typed after the requested encoding, which is a string here.
        val name = entry.name.unsafeCast<String>()
        val path = if (directory == ".") name else "$directory/$name"
        when {
            entry.isDirectory() && !isSkipped(name) -> findBuildScripts(path)
            entry.isFile() && name == ROOT_BUILD -> listOf(path)
            else -> emptyList()
        }
    }
