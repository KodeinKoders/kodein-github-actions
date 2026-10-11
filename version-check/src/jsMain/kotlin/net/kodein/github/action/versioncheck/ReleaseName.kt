package net.kodein.github.action.versioncheck


// {Major}.{Minor}.{Patch}, maybe followed by -{Tag}. Numbers have no leading zero.
private val releaseNameRegex = Regex("""^v((?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)(?:-[0-9A-Za-z_][0-9A-Za-z_.-]*)?)$""")

/** Returns the version a release name stands for (`v3.5.2` -> `3.5.2`), or null if it is not a valid release name. */
internal fun parseReleaseVersion(releaseName: String): String? =
    releaseNameRegex.matchEntire(releaseName)?.groupValues?.get(1)

internal fun checkReleaseName(releaseName: String): List<Problem> =
    if (parseReleaseVersion(releaseName) != null) emptyList()
    else listOf(Problem("Release name \"$releaseName\" must be \"v{Major}.{Minor}.{Patch}\", optionally followed by \"-{Tag}\"."))

/** The version without its patch: `3.5.2` -> `3.5`, and `3.5.2-RC1` -> `3.5`. */
internal fun displayVersionOf(version: String): String = version.split('.').take(2).joinToString(".")
