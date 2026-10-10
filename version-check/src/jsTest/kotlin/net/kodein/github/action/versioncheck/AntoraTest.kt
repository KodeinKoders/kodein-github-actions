package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class AntoraTest {

    private val catalog = mapOf("kotlin" to "2.4.10", "ktor" to "3.1.0", "guava" to null)

    @Test
    fun valid() {
        assertEquals(emptyList(), checkAntora("""
            name: my-lib
            title: My Lib
            asciidoc:
              attributes:
                kotlin-version: 2.4.10
                ktor-version: "3.1.0"
                something: else
        """.trimIndent(), catalog))
    }

    @Test
    fun withoutAsciidoc() {
        assertEquals(emptyList(), checkAntora("name: my-lib\n", catalog))
    }

    @Test
    fun rootVersionAndDisplayVersion() {
        val problems = checkAntora("name: my-lib\nversion: '3.5'\ndisplay_version: 3.5.2\n", catalog)
        assertEquals(listOf("version", "display_version").map { "doc/antora.yml must not have the root \"$it\" property." }, problems.map { it.message })
        assertEquals(listOf(2, 3), problems.map { it.line })
    }

    @Test
    fun mismatchedAttribute() {
        val problems = checkAntora("asciidoc:\n  attributes:\n    kotlin-version: 2.4.0\n", catalog)
        assertEquals(1, problems.size)
        assertEquals(3, problems.single().line)
        assertTrue("2.4.0" in problems.single().message && "2.4.10" in problems.single().message)
    }

    @Test
    fun unknownAndNonPlainAttributes() {
        val problems = checkAntora("asciidoc:\n  attributes:\n    foo-version: 1\n    guava-version: 33.0\n", catalog)
        assertEquals(2, problems.size)
        assertTrue("\"foo\"" in problems[0].message)
        assertTrue("not a plain string" in problems[1].message)
    }

    @Test
    fun invalidYaml() {
        assertEquals(1, checkAntora("a: [unclosed\n", catalog).size)
    }
}
