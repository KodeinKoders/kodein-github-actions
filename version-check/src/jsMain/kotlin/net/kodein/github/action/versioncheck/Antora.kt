package net.kodein.github.action.versioncheck

import yaml.parse


internal const val ANTORA_FILE = "doc/antora.yml"

private fun keysOf(obj: dynamic): List<String> = js("Object.keys(obj)").unsafeCast<Array<String>>().toList()

private fun lineOf(text: String, regex: Regex): Int? =
    regex.find(text)?.let { text.substring(0, it.range.first).count { c -> c == '\n' } + 1 }

// A YAML scalar such as 3.10 is a number, and reads as 3.1: only a string can hold the version as written.
private fun checkVersionProperty(name: String, value: dynamic, expected: String?, line: Int?): List<Problem> = when {
    value == null ->
        listOf(Problem("$ANTORA_FILE must have the \"$name\" property.", ANTORA_FILE))
    jsTypeOf(value) != "string" ->
        listOf(Problem("\"$name\" in $ANTORA_FILE must be a quoted string, so that YAML does not read it as a number.", ANTORA_FILE, line))
    expected != null && value.unsafeCast<String>() != expected ->
        listOf(Problem("\"$name\" is \"$value\" in $ANTORA_FILE, but it must be \"$expected\" for this release.", ANTORA_FILE, line))
    else -> emptyList()
}

/**
 * Checks `doc/antora.yml` against the release [version] (null if the release name is invalid, which is reported
 * elsewhere) and against the version [catalog], which was read from [catalogPath] (only used in the problems).
 */
internal fun checkAntora(text: String, catalog: Map<String, String?>, version: String?, catalogPath: String = VERSION_CATALOG): List<Problem> {
    val root: dynamic = try {
        parse<JsAny?>(text).unsafeCast<dynamic>()
    } catch (e: Throwable) {
        return listOf(Problem("$ANTORA_FILE is not valid YAML: ${e.message}", ANTORA_FILE))
    }
    if (root == null || jsTypeOf(root) != "object") {
        return listOf(Problem("$ANTORA_FILE must be a YAML mapping.", ANTORA_FILE))
    }

    return buildList {
        val displayVersion = version?.let { displayVersionOf(it) }
        addAll(checkVersionProperty("version", root.version, version, lineOf(text, Regex("^version\\s*:", RegexOption.MULTILINE))))
        addAll(checkVersionProperty("display_version", root.display_version, displayVersion, lineOf(text, Regex("^display_version\\s*:", RegexOption.MULTILINE))))

        val attributes: dynamic = root.asciidoc?.attributes
        val attributesObject = attributes != null && jsTypeOf(attributes) == "object"
        addAll(checkVersionProperty("asciidoc.attributes.version", if (attributesObject) attributes.version else null, version, lineOf(text, Regex("^\\s+version\\s*:", RegexOption.MULTILINE))))
        if (!attributesObject) return@buildList
        for (attribute in keysOf(attributes).filter { it.endsWith("-version") }) {
            val line = lineOf(text, Regex("^\\s+${Regex.escape(attribute)}\\s*:", RegexOption.MULTILINE))
            val alias = attribute.removeSuffix("-version")
            val actual = attributes[attribute].toString()
            when {
                alias !in catalog ->
                    add(Problem("Attribute \"$attribute\" has no \"$alias\" version in $catalogPath.", ANTORA_FILE, line))
                catalog[alias] == null ->
                    add(Problem("Version \"$alias\" in $catalogPath is not a plain string, so \"$attribute\" cannot be checked.", ANTORA_FILE, line))
                catalog[alias] != actual ->
                    add(Problem("Attribute \"$attribute\" is \"$actual\" but \"$alias\" is \"${catalog[alias]}\" in $catalogPath.", ANTORA_FILE, line))
            }
        }
    }
}
