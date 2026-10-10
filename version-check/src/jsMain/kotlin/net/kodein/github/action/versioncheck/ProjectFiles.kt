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

/** Every `build.gradle.kts` in the repository except the root one. */
internal fun findOtherBuildScripts(directory: String = "."): List<String> =
    readdirSync(directory, ReaddirSyncWithFileTypesOptions(withFileTypes = true)).flatMap { entry ->
        // Entries are typed after the requested encoding, which is a string here.
        val name = entry.name.unsafeCast<String>()
        val path = if (directory == ".") name else "$directory/$name"
        when {
            entry.isDirectory() && !isSkipped(name) -> findOtherBuildScripts(path)
            entry.isFile() && name == "build.gradle.kts" && directory != "." -> listOf(path)
            else -> emptyList()
        }
    }
