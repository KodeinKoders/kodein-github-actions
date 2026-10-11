package net.kodein.github.action.releasebranches

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ReleaseNameTest {

    @Test
    fun valid() {
        assertEquals(ReleaseVersion("3", "5", "2", null), parseReleaseName("3.5.2"))
        assertEquals(ReleaseVersion("3", "5", "2", "RC1"), parseReleaseName("3.5.2-RC1"))
        assertEquals(ReleaseVersion("0", "10", "0", "beta.2"), parseReleaseName("0.10.0-beta.2"))
    }

    @Test
    fun invalid() {
        for (name in listOf("v3.5.2", "3.5", "3.5.2-", "3.5.2.1", "03.5.2", "V3.5.2", "3.5.2 ", "release-3.5.2", "")) {
            assertNull(parseReleaseName(name), "\"$name\" should be rejected")
        }
    }
}
