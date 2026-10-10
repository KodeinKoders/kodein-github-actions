package net.kodein.github.action.versioncheck


internal const val VERSION_CATALOG = "gradle/libs.versions.toml"

/**
 * The `[versions]` table of a Gradle version catalog, by alias.
 *
 * A version that is not a plain string (`{ strictly = "..." }`) maps to null.
 */
internal fun parseCatalogVersions(text: String): Map<String, String?> {
    val versions = mutableMapOf<String, String?>()
    var inVersions = false
    for (line in text.lines()) {
        val section = sectionRegex.matchEntire(line)
        if (section != null) {
            inVersions = section.groupValues[1].trim() == "versions"
            continue
        }
        if (!inVersions) continue
        plainRegex.matchEntire(line)?.let { versions[it.groupValues[1]] = it.groupValues[2] }
            ?: richRegex.matchEntire(line)?.let { versions[it.groupValues[1]] = null }
    }
    return versions
}

// Escaped everywhere: Kotlin/JS compiles regexes in unicode mode, where a bare "]" is a syntax error.
private val sectionRegex = Regex("""^\s*\[([^\[\]]+)\]\s*(?:#.*)?$""")
private val plainRegex = Regex("""^\s*([\w.-]+)\s*=\s*"([^"]*)"\s*(?:#.*)?$""")
private val richRegex = Regex("""^\s*([\w.-]+)\s*=\s*\{.*$""")
