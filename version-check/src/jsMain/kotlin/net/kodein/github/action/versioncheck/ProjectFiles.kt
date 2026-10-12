package net.kodein.github.action.versioncheck

import node.fs.existsSync
import node.fs.readFileSync
import node.fs.rmSync
import node.fs.writeFileSync


// All paths are relative to the working path, which is the workspace once the repository is checked out.

internal fun exists(path: String): Boolean = existsSync(path)

internal fun readText(path: String): String = readFileSync(path, "utf8".unsafeCast<node.buffer.BufferEncoding>())

internal fun writeText(path: String, text: String) = writeFileSync(path, text)

internal fun delete(path: String) = rmSync(path)

/** Whether [path] designates the workspace itself, as opposed to a subpath of it. */
internal fun isWorkspaceRoot(path: String): Boolean = normalized(path).let { it.isEmpty() || it == "." }

private fun normalized(path: String) = path.trim().removePrefix("./").trimEnd('/')

/** The path of [file] in the Gradle build that lives in [path], relative to the workspace. */
private fun pathIn(path: String, file: String): String =
    if (isWorkspaceRoot(path)) file else "${normalized(path)}/$file"

internal fun initScriptPath(path: String): String = pathIn(path, INIT_SCRIPT)

internal fun versionCatalogPath(path: String): String = pathIn(path, VERSION_CATALOG)
