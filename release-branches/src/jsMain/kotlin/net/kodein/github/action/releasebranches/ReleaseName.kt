package net.kodein.github.action.releasebranches


internal data class ReleaseVersion(
    val major: String,
    val minor: String,
    val patch: String,
    val tag: String?,
)

// {Major}.{Minor}.{Patch}, maybe followed by -{Tag}. Numbers have no leading zero.
private val releaseNameRegex = Regex("""^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([0-9A-Za-z_][0-9A-Za-z_.-]*))?$""")

/** Parses a release name such as `3.5.2` or `3.5.2-RC1`, or returns null if it is not one. */
internal fun parseReleaseName(releaseName: String): ReleaseVersion? =
    releaseNameRegex.matchEntire(releaseName)?.groupValues?.let { (_, major, minor, patch, tag) ->
        ReleaseVersion(major, minor, patch, tag.ifEmpty { null })
    }
