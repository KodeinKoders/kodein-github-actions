package net.kodein.github.action.versioncheck


// {Major}.{Minor}.{Patch}, maybe followed by -{Tag}. Numbers have no leading zero.
private val releaseNameRegex = Regex("""^((?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)(?:-[0-9A-Za-z_][0-9A-Za-z_.-]*)?)$""")

/** Returns the version a release name stands for, which is the name itself, or null if it is not a valid release name. */
internal fun parseReleaseVersion(releaseName: String): String? =
    releaseNameRegex.matchEntire(releaseName)?.groupValues?.get(1)

internal fun checkReleaseName(releaseName: String): List<Problem> =
    if (parseReleaseVersion(releaseName) != null) emptyList()
    else listOf(Problem("Release name \"$releaseName\" must be \"{Major}.{Minor}.{Patch}\", optionally followed by \"-{Tag}\"."))

/** The version without its patch: `3.5.2` -> `3.5`, and `3.5.2-RC1` -> `3.5`. */
internal fun shortVersionOf(version: String): String = version.split('.').take(2).joinToString(".")

/** The tag of a release is its name prefixed by `v`: release `3.5.2` is tagged `v3.5.2`. */
internal fun checkTagName(tagName: String, releaseName: String): List<Problem> =
    if (tagName == "v$releaseName") emptyList()
    else listOf(Problem("Tag name \"$tagName\" must be \"v$releaseName\", the release name prefixed by \"v\"."))
