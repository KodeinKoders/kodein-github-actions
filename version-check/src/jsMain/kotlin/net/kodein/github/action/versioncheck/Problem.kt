package net.kodein.github.action.versioncheck


/** A failed check, reported as one GitHub annotation. */
internal data class Problem(
    val message: String,
    val file: String? = null,
    val line: Int? = null,
)
