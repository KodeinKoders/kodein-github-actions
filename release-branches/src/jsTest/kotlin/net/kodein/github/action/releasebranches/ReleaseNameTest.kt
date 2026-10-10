package net.kodein.github.action.releasebranches

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ReleaseNameTest {

    @Test
    fun valid() {
        assertEquals(ReleaseVersion("3", "5", "2", null), parseReleaseName("v3.5.2"))
        assertEquals(ReleaseVersion("3", "5", "2", "RC1"), parseReleaseName("v3.5.2-RC1"))
        assertEquals(ReleaseVersion("0", "10", "0", "beta.2"), parseReleaseName("v0.10.0-beta.2"))
    }

    @Test
    fun invalid() {
        for (name in listOf("3.5.2", "v3.5", "v3.5.2-", "v3.5.2.1", "v03.5.2", "V3.5.2", "v3.5.2 ", "release-3.5.2", "")) {
            assertNull(parseReleaseName(name), "\"$name\" should be rejected")
        }
    }
}
