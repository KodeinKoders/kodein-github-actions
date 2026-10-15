package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals


class ReadmeTest {

    @Test
    fun valid() {
        assertEquals(emptyList(), checkReadme("= My Lib\n:version: 3.5.2\n:version-branch: v3.5\n:version-tag: v3.5.2\n\nText", "3.5.2"))
        assertEquals(emptyList(), checkReadme("= My Lib\n:version: 3.5.2-RC1\n:version-branch: v3.5\n:version-tag: v3.5.2-RC1\n", "3.5.2-RC1"))
    }

    @Test
    fun attributesAreOptional() {
        assertEquals(emptyList(), checkReadme("= My Lib\n:other: 3.5.2\n", "3.5.2"))
        assertEquals(emptyList(), checkReadme("= My Lib\n:version: 3.5.2\n", "3.5.2"))
    }

    @Test
    fun wrong() {
        val problems = checkReadme("= My Lib\n:version: 3.5\n:version-branch: 3.5\n:version-tag: v3.5.1\n", "3.5.2")
        assertEquals(listOf(2, 3, 4), problems.map { it.line })
        assertEquals(
            listOf(
                "The \"version\" attribute is \"3.5\" in README.adoc, but it must be \"3.5.2\" for this release.",
                "The \"version-branch\" attribute is \"3.5\" in README.adoc, but it must be \"v3.5\" for this release.",
                "The \"version-tag\" attribute is \"v3.5.1\" in README.adoc, but it must be \"v3.5.2\" for this release.",
            ),
            problems.map { it.message },
        )
    }

    @Test
    fun invalidReleaseNameChecksNothing() {
        assertEquals(emptyList(), checkReadme(":version: x\n:version-tag: y\n", null))
    }
}
