package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class GradleChecksTest {

    private fun root(body: String) = checkRootBuild(body, "3.5.2")

    @Test
    fun rootValid() {
        assertEquals(emptyList(), root("""
            plugins { alias(libs.plugins.kotlin.multiplatform) apply false }

            allprojects {
                group = "org.kodein"
                version = "3.5.2"
            }
        """.trimIndent()))
    }

    @Test
    fun rootWithoutAllProjects() {
        val problems = root("""subprojects { version = "3.5.2" }""")
        assertEquals(1, problems.size)
        assertTrue("allprojects" in problems.single().message)
    }

    @Test
    fun rootWithoutVersion() {
        val problems = root("allprojects {\n    group = \"org.kodein\"\n}")
        assertEquals(1, problems.size)
        assertTrue("version" in problems.single().message)
    }

    @Test
    fun rootMismatch() {
        val problems = root("allprojects {\n    group = \"x\"\n    version = \"3.5.1\"\n}")
        assertEquals(1, problems.size)
        assertEquals(3, problems.single().line)
        assertTrue("must be named \"3.5.1\"" in problems.single().message)
    }

    @Test
    fun rootNotALiteral() {
        val problems = root("""allprojects { version = "3.5.${'$'}{patch}" }""")
        assertEquals(1, problems.size)
        assertTrue("literal" in problems.single().message)
    }

    @Test
    fun rootSeveralAssignments() {
        val problems = root("allprojects {\n    version = \"3.5.2\"\n    version = \"3.5.2\"\n}")
        assertEquals(1, problems.size)
        assertEquals(3, problems.single().line)
    }

    @Test
    fun commentsAndStringsAreIgnored() {
        val problems = root("""
            // allprojects { version = "9.9.9" }
            /* version = "8.8.8" */
            val url = "https://example.org/allprojects { version = 7 }"
            allprojects {
                // version = "1.1.1"
                val doc = ${"\"\"\""}
                    version = "2.2.2"
                ${"\"\"\""}
                version = "3.5.2" // the real one
            }
        """.trimIndent())
        assertEquals(emptyList(), problems)
    }

    @Test
    fun nestedBlocksAndTemplates() {
        assertEquals(emptyList(), root("""
            val x = "${'$'}{ mapOf("a" to "}") }"
            allprojects {
                run { println("{") }
                version = "3.5.2"
            }
        """.trimIndent()))
    }

    @Test
    fun allProjectsMustBeTopLevel() {
        val problems = root("""
            subprojects {
                allprojects { version = "3.5.2" }
            }
        """.trimIndent())
        assertTrue("allprojects" in problems.single().message)
    }

    @Test
    fun otherBuildsMustNotAssignVersion() {
        val problems = checkOtherBuild("lib/build.gradle.kts", "plugins {}\n\nversion = \"1.0\"\n")
        assertEquals(listOf(Problem("Only the root build.gradle.kts may assign \"version\".", "lib/build.gradle.kts", 3)), problems)
    }

    @Test
    fun otherBuildsMayUseTheWordVersion() {
        assertEquals(emptyList(), checkOtherBuild("lib/build.gradle.kts", """
            val version = "1.0"
            dependencies { implementation(libs.foo) }
            mavenPublishing { pom { description = "version = 1" } }
            foo(version = "1.0")
            if (version == "2") {}
            libs.versions.kotlin.get()
        """.trimIndent()))
    }
}
