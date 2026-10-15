package net.kodein.github.action.versioncheck


internal const val README_FILE = "README.adoc"

private val versionAttributeRegex = Regex("^:version:[ \\t]*(.*?)[ \\t]*$", RegexOption.MULTILINE)

/** Checks that [text], the content of `README.adoc`, has a `version` attribute, which is the release [version] (null if the release name is invalid, which is reported elsewhere). */
internal fun checkReadme(text: String, version: String?): List<Problem> {
    val match = versionAttributeRegex.find(text)
        ?: return listOf(Problem("$README_FILE must have a \":version:\" attribute.", README_FILE))
    val actual = match.groupValues[1]
    if (version == null || actual == version) return emptyList()
    val line = text.substring(0, match.range.first).count { it == '\n' } + 1
    return listOf(Problem("The \"version\" attribute is \"$actual\" in $README_FILE, but it must be \"$version\" for this release.", README_FILE, line))
}
