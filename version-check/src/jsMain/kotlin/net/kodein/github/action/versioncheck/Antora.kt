package net.kodein.github.action.versioncheck

import yaml.parse


internal const val ANTORA_FILE = "doc/antora.yml"

private fun keysOf(obj: dynamic): List<String> = js("Object.keys(obj)").unsafeCast<Array<String>>().toList()

private fun lineOf(text: String, regex: Regex): Int? =
    regex.find(text)?.let { text.substring(0, it.range.first).count { c -> c == '\n' } + 1 }

internal fun checkAntora(text: String, catalog: Map<String, String?>): List<Problem> {
    val root: dynamic = try {
        parse<JsAny?>(text).unsafeCast<dynamic>()
    } catch (e: Throwable) {
        return listOf(Problem("$ANTORA_FILE is not valid YAML: ${e.message}", ANTORA_FILE))
    }
    if (root == null || jsTypeOf(root) != "object") {
        return listOf(Problem("$ANTORA_FILE must be a YAML mapping.", ANTORA_FILE))
    }

    return buildList {
        // The version comes from the release (the tag), never from the component descriptor.
        val rootKeys = keysOf(root)
        for (key in listOf("version", "display_version")) {
            if (key in rootKeys) {
                add(Problem("$ANTORA_FILE must not have the root \"$key\" property.", ANTORA_FILE, lineOf(text, Regex("^$key\\s*:", RegexOption.MULTILINE))))
            }
        }

        val attributes: dynamic = root.asciidoc?.attributes
        if (attributes == null || jsTypeOf(attributes) != "object") return@buildList
        for (attribute in keysOf(attributes).filter { it.endsWith("-version") }) {
            val line = lineOf(text, Regex("^\\s+${Regex.escape(attribute)}\\s*:", RegexOption.MULTILINE))
            val alias = attribute.removeSuffix("-version")
            val actual = attributes[attribute].toString()
            when {
                alias !in catalog ->
                    add(Problem("Attribute \"$attribute\" has no \"$alias\" version in $VERSION_CATALOG.", ANTORA_FILE, line))
                catalog[alias] == null ->
                    add(Problem("Version \"$alias\" in $VERSION_CATALOG is not a plain string, so \"$attribute\" cannot be checked.", ANTORA_FILE, line))
                catalog[alias] != actual ->
                    add(Problem("Attribute \"$attribute\" is \"$actual\" but \"$alias\" is \"${catalog[alias]}\" in $VERSION_CATALOG.", ANTORA_FILE, line))
            }
        }
    }
}
