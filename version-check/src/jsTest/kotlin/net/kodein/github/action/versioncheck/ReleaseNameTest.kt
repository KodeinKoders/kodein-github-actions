package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ReleaseNameTest {

    @Test
    fun valid() {
        assertEquals("3.5.2", parseReleaseVersion("3.5.2"))
        assertEquals("3.5.2-RC1", parseReleaseVersion("3.5.2-RC1"))
        assertEquals("0.10.0-beta.2", parseReleaseVersion("0.10.0-beta.2"))
    }

    @Test
    fun shortVersion() {
        assertEquals("3.5", shortVersionOf("3.5.2"))
        assertEquals("3.5", shortVersionOf("3.5.2-RC1"))
        assertEquals("0.10", shortVersionOf("0.10.0-beta.2"))
    }

    @Test
    fun invalid() {
        for (name in listOf("v3.5.2", "3.5", "3.5.2-", "3.5.2.1", "03.5.2", "V3.5.2", "3.5.2 ", "release-3.5.2", "")) {
            assertNull(parseReleaseVersion(name), "\"$name\" should be rejected")
            assertEquals(1, checkReleaseName(name).size)
        }
    }

    @Test
    fun tagName() {
        assertEquals(emptyList(), checkTagName("v3.5.2", "3.5.2"))
        assertEquals(emptyList(), checkTagName("v3.5.2-RC1", "3.5.2-RC1"))
        for (tag in listOf("3.5.2", "v3.5.3", "v3.5.2-RC1", "V3.5.2", "vv3.5.2", "")) {
            assertEquals(
                listOf("Tag name \"$tag\" must be \"v3.5.2\", the release name prefixed by \"v\"."),
                checkTagName(tag, "3.5.2").map { it.message },
            )
        }
    }
}
