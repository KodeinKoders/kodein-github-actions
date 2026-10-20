package net.kodein.github.action.versioncheck


internal const val README_FILE = "README.adoc"

private fun checkAttribute(text: String, name: String, expected: String?): List<Problem> {
    val match = Regex("^:${Regex.escape(name)}:[ \\t]*(.*?)[ \\t]*$", RegexOption.MULTILINE).find(text) ?: return emptyList()
    val actual = match.groupValues[1]
    if (expected == null || actual == expected) return emptyList()
    val line = text.substring(0, match.range.first).count { it == '\n' } + 1
    return listOf(Problem("The \"$name\" attribute is \"$actual\" in $README_FILE, but it must be \"$expected\" for this release.", README_FILE, line))
}

/**
 * Checks the optional `version`, `version-branch` and `version-tag` attributes of [text], the content of `README.adoc`,
 * against the release [version] (null if the release name is invalid, which is reported elsewhere).
 * An attribute that is not there is fine; one that is there must have the expected value.
 */
internal fun checkReadme(text: String, version: String?): List<Problem> = buildList {
    addAll(checkAttribute(text, "version", version))
    addAll(checkAttribute(text, "version-branch", version?.let { "v${shortVersionOf(it)}" }))
    addAll(checkAttribute(text, "version-tag", version?.let { "v$it" }))
}
