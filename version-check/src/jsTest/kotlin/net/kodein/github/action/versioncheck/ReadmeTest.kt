package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals


class ReadmeTest {

    @Test
    fun valid() {
        assertEquals(emptyList(), checkReadme("= My Lib\n:version: 3.5.2\n\nText", "3.5.2"))
        assertEquals(emptyList(), checkReadme("= My Lib\n:version: 3.5.2-RC1\n", "3.5.2-RC1"))
    }

    @Test
    fun missing() {
        assertEquals(listOf("README.adoc must have a \":version:\" attribute."), checkReadme("= My Lib\n:other: 3.5.2\n", "3.5.2").map { it.message })
    }

    @Test
    fun wrong() {
        val problems = checkReadme("= My Lib\n:version: 3.5\n", "3.5.2")
        assertEquals(listOf(2), problems.map { it.line })
        assertEquals(listOf("The \"version\" attribute is \"3.5\" in README.adoc, but it must be \"3.5.2\" for this release."), problems.map { it.message })
    }

    @Test
    fun invalidReleaseNameOnlyChecksPresence() {
        assertEquals(emptyList(), checkReadme(":version: x\n", null))
        assertEquals(1, checkReadme("= My Lib\n", null).size)
    }
}
