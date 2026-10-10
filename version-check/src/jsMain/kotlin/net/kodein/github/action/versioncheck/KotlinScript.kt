package net.kodein.github.action.versioncheck


internal class VersionAssignment(
    /** 1-based. */
    val line: Int,
    /** The string literal assigned, or null if the right-hand side is anything else (a variable, a template, ...). */
    val value: String?,
    /** The right-hand side as written, for messages. */
    val expression: String,
)

/**
 * Just enough of a Kotlin script reader to find `name { }` blocks and `version = ...` assignments.
 *
 * Comments and string contents are masked out first, so a brace or a `version =` inside a comment or a string
 * (think `"https://..."`) is never mistaken for code.
 */
internal class KotlinScript(private val text: String) {

    // Same length as `text`, with comments and string contents replaced by spaces - so offsets are shared.
    private val masked = Masker(text).mask()

    private fun lineOf(offset: Int): Int = text.substring(0, offset).count { it == '\n' } + 1

    private fun matchingBrace(open: Int): Int {
        var depth = 0
        for (i in open until masked.length) {
            when (masked[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return i
            }
        }
        return masked.length
    }

    private fun depthAt(offset: Int): Int {
        var depth = 0
        for (i in 0 until offset) {
            when (masked[i]) {
                '{' -> depth++
                '}' -> depth--
            }
        }
        return depth
    }

    /** The bodies of every `name { ... }` block at the top level of the script. */
    fun topLevelBlocks(name: String): List<IntRange> =
        Regex("""(?<![\w.])${Regex.escape(name)}\s*\{""").findAll(masked)
            .filter { depthAt(it.range.first) == 0 }
            .map { (it.range.last + 1)..<matchingBrace(it.range.last) }
            .toList()

    fun versionAssignments(within: IntRange = masked.indices): List<VersionAssignment> =
        assignmentRegex.findAll(masked)
            .filter { it.range.first in within }
            .filterNot { isDeclarationOrNamedArgument(it.range.first) }
            .map { match ->
                var i = match.range.last + 1
                while (i < masked.length && masked[i].isWhitespace()) i++
                val lineEnd = masked.indexOf('\n', i).let { if (it < 0) masked.length else it }
                val expression = text.substring(i, lineEnd).substringBefore("//").trim()
                VersionAssignment(lineOf(match.range.first), literalAt(i), expression)
            }
            .toList()

    // The string literal starting at `start`, if it is a plain one (no templates, no escapes).
    private fun literalAt(start: Int): String? {
        if (masked.getOrNull(start) != '"' || masked.startsWith("\"\"", start)) return null
        val close = masked.indexOf('"', start + 1)
        if (close < 0) return null
        val raw = text.substring(start + 1, close)
        return raw.takeIf { '$' !in it && '\\' !in it }
    }

    // `val version = ...` declares a variable, `foo(version = ...)` names an argument: neither sets a version.
    private fun isDeclarationOrNamedArgument(start: Int): Boolean {
        val before = masked.substring(0, start).trimEnd()
        return before.endsWith("(") || before.endsWith(",") || declarationRegex.containsMatchIn(before)
    }

    private companion object {
        val assignmentRegex = Regex("""(?<!\w)version\s*=(?!=)""")
        val declarationRegex = Regex("""\b(?:val|var)$""")
    }
}


private class Masker(private val text: String) {
    private val out = text.toCharArray()
    private var i = 0

    fun mask(): String {
        code(blank = false, untilBrace = false)
        return out.concatToString()
    }

    private fun blankAt(index: Int) {
        if (out[index] != '\n') out[index] = ' '
    }

    private fun code(blank: Boolean, untilBrace: Boolean) {
        var depth = 0
        while (i < text.length) {
            when {
                text.startsWith("//", i) -> {
                    while (i < text.length && text[i] != '\n') blankAt(i++)
                }
                text.startsWith("/*", i) -> blockComment()
                text.startsWith("\"\"\"", i) -> { i += 3 ; string(raw = true) }
                text[i] == '"' -> { i++ ; string(raw = false) }
                text[i] == '\'' -> charLiteral()
                else -> {
                    if (untilBrace) {
                        if (text[i] == '{') depth++
                        if (text[i] == '}' && depth-- == 0) return
                    }
                    if (blank) blankAt(i)
                    i++
                }
            }
        }
    }

    // Kotlin block comments nest.
    private fun blockComment() {
        var depth = 0
        while (i < text.length) {
            when {
                text.startsWith("/*", i) -> { depth++ ; blankAt(i++) ; blankAt(i++) }
                text.startsWith("*/", i) -> { blankAt(i++) ; blankAt(i++) ; if (--depth == 0) return }
                else -> blankAt(i++)
            }
        }
    }

    private fun charLiteral() {
        i++
        while (i < text.length && text[i] != '\'') {
            if (text[i] == '\\') blankAt(i++)
            if (i < text.length) blankAt(i++)
        }
        i++
    }

    // The opening delimiter is already consumed; the closing one is kept, the contents are blanked.
    private fun string(raw: Boolean) {
        while (i < text.length) {
            when {
                raw && text.startsWith("\"\"\"", i) -> { i += 3 ; return }
                !raw && text[i] == '"' -> { i++ ; return }
                !raw && text[i] == '\\' -> { blankAt(i++) ; if (i < text.length) blankAt(i++) }
                text.startsWith("\${", i) -> {
                    // A template holds code, which can itself hold strings and braces.
                    blankAt(i++) ; blankAt(i++)
                    code(blank = true, untilBrace = true)
                    if (i < text.length) blankAt(i++)
                }
                else -> blankAt(i++)
            }
        }
    }
}
