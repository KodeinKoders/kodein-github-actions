package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals


class VersionCatalogTest {

    @Test
    fun onlyTheVersionsTable() {
        val versions = parseCatalogVersions("""
            [versions]
            kotlin = "2.4.10"                   # https://kotlinlang.org/docs/releases.html
            kotlinWrappers-yaml = "2026.9.3-2.8.3"
            guava = { strictly = "33.0" }

            # a comment
            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }

            [libraries]
            kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
        """.trimIndent())
        assertEquals(mapOf("kotlin" to "2.4.10", "kotlinWrappers-yaml" to "2026.9.3-2.8.3", "guava" to null), versions)
    }
}
