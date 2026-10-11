package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ReleaseNameTest {

    @Test
    fun valid() {
        assertEquals("3.5.2", parseReleaseVersion("v3.5.2"))
        assertEquals("3.5.2-RC1", parseReleaseVersion("v3.5.2-RC1"))
        assertEquals("0.10.0-beta.2", parseReleaseVersion("v0.10.0-beta.2"))
    }

    @Test
    fun displayVersion() {
        assertEquals("3.5", displayVersionOf("3.5.2"))
        assertEquals("3.5", displayVersionOf("3.5.2-RC1"))
        assertEquals("0.10", displayVersionOf("0.10.0-beta.2"))
    }

    @Test
    fun invalid() {
        for (name in listOf("3.5.2", "v3.5", "v3.5.2-", "v3.5.2.1", "v03.5.2", "V3.5.2", "v3.5.2 ", "release-3.5.2", "")) {
            assertNull(parseReleaseVersion(name), "\"$name\" should be rejected")
            assertEquals(1, checkReleaseName(name).size)
        }
    }
}
