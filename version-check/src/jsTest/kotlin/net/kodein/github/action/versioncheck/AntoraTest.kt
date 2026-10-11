package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class AntoraTest {

    private val catalog = mapOf("kotlin" to "2.4.10", "ktor" to "3.1.0", "guava" to null)

    private val versions = """
        name: my-lib
        version: 3.5.2
        display_version: '3.5'
    """.trimIndent()

    @Test
    fun valid() {
        assertEquals(emptyList(), checkAntora(versions + "\n" + """
            title: My Lib
            asciidoc:
              attributes:
                version: 3.5.2
                kotlin-version: 2.4.10
                ktor-version: "3.1.0"
                something: else
        """.trimIndent(), catalog, "3.5.2"))
    }

    @Test
    fun preRelease() {
        assertEquals(emptyList(), checkAntora("""
            version: 3.5.2-RC1
            display_version: '3.5'
            asciidoc:
              attributes:
                version: 3.5.2-RC1
        """.trimIndent(), catalog, "3.5.2-RC1"))
    }

    @Test
    fun missingProperties() {
        val problems = checkAntora("name: my-lib\n", catalog, "3.5.2")
        assertEquals(
            listOf("version", "display_version", "asciidoc.attributes.version").map { "doc/antora.yml must have the \"$it\" property." },
            problems.map { it.message },
        )
    }

    @Test
    fun missingAttributeVersion() {
        val problems = checkAntora("$versions\nasciidoc:\n  attributes:\n    kotlin-version: 2.4.10\n", catalog, "3.5.2")
        assertEquals(listOf("doc/antora.yml must have the \"asciidoc.attributes.version\" property."), problems.map { it.message })
    }

    @Test
    fun wrongVersions() {
        val problems = checkAntora("""
            version: 3.5.1
            display_version: '3.6'
            asciidoc:
              attributes:
                version: 3.4.2
        """.trimIndent(), catalog, "3.5.2")
        assertEquals(listOf(1, 2, 5), problems.map { it.line })
        assertEquals(
            listOf(
                "\"version\" is \"3.5.1\" in doc/antora.yml, but it must be \"3.5.2\" for this release.",
                "\"display_version\" is \"3.6\" in doc/antora.yml, but it must be \"3.5\" for this release.",
                "\"asciidoc.attributes.version\" is \"3.4.2\" in doc/antora.yml, but it must be \"3.5.2\" for this release.",
            ),
            problems.map { it.message },
        )
    }

    @Test
    fun displayVersionMustBeQuoted() {
        // Unquoted, 3.10 is the number 3.1.
        val problems = checkAntora("version: 3.10.2\ndisplay_version: 3.10\nasciidoc:\n  attributes:\n    version: 3.10.2\n", catalog, "3.10.2")
        assertEquals(1, problems.size)
        assertEquals(2, problems.single().line)
        assertTrue("quoted string" in problems.single().message)
    }

    @Test
    fun invalidReleaseNameOnlyChecksPresence() {
        assertEquals(emptyList(), checkAntora("version: x\ndisplay_version: x\nasciidoc:\n  attributes:\n    version: x\n", catalog, null))
        assertEquals(3, checkAntora("name: my-lib\n", catalog, null).size)
    }

    @Test
    fun mismatchedAttribute() {
        val problems = checkAntora("$versions\nasciidoc:\n  attributes:\n    version: 3.5.2\n    kotlin-version: 2.4.0\n", catalog, "3.5.2")
        assertEquals(1, problems.size)
        assertEquals(7, problems.single().line)
        assertTrue("2.4.0" in problems.single().message && "2.4.10" in problems.single().message)
    }

    @Test
    fun unknownAndNonPlainAttributes() {
        val problems = checkAntora("$versions\nasciidoc:\n  attributes:\n    version: 3.5.2\n    foo-version: 1\n    guava-version: 33.0\n", catalog, "3.5.2")
        assertEquals(2, problems.size)
        assertTrue("\"foo\"" in problems[0].message)
        assertTrue("not a plain string" in problems[1].message)
    }

    @Test
    fun invalidYaml() {
        assertEquals(1, checkAntora("a: [unclosed\n", catalog, "3.5.2").size)
    }
}
