package net.kodein.github.action.versioncheck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class GradleCheckTest {

    @Test
    fun initScript() {
        assertEquals("kodein_initScript_checkVersion.init.gradle.kts", INIT_SCRIPT)
        assertTrue("tasks.register(\"kodein_initScript_checkVersion\")" in INIT_SCRIPT_CONTENT)
        assertTrue("project.providers.gradleProperty(\"expectedVersion\").get()" in INIT_SCRIPT_CONTENT)
        assertTrue("Root project has version '\${project.version}', expected '\$expectedVersion'" in INIT_SCRIPT_CONTENT)
        assertTrue("Project \${this.name} has version '\${this.version}', root project has version '\$expectedVersion'" in INIT_SCRIPT_CONTENT)
    }

    @Test
    fun argumentsAtTheRoot() {
        assertEquals(
            listOf("--no-configuration-cache", "--no-daemon", "-I", "kodein_initScript_checkVersion.init.gradle.kts", "-PexpectedVersion=3.5.2", "kodein_initScript_checkVersion", "-q"),
            gradleArguments(".", "3.5.2"),
        )
        assertEquals(gradleArguments(".", "3.5.2"), gradleArguments("", "3.5.2"))
        assertEquals(gradleArguments(".", "3.5.2"), gradleArguments("./", "3.5.2"))
    }

    @Test
    fun argumentsInASubdirectory() {
        assertEquals(
            listOf("--no-configuration-cache", "--no-daemon", "-p", "mockmp", "-I", "kodein_initScript_checkVersion.init.gradle.kts", "-PexpectedVersion=3.5.2-RC1", "kodein_initScript_checkVersion", "-q"),
            gradleArguments("mockmp", "3.5.2-RC1"),
        )
    }

    @Test
    fun initScriptPaths() {
        assertEquals("kodein_initScript_checkVersion.init.gradle.kts", initScriptPath("."))
        assertEquals("mockmp/kodein_initScript_checkVersion.init.gradle.kts", initScriptPath("./mockmp/"))
        assertEquals("a/b/kodein_initScript_checkVersion.init.gradle.kts", initScriptPath("a/b"))
    }

    @Test
    fun versionCatalogPaths() {
        assertEquals("gradle/libs.versions.toml", versionCatalogPath("."))
        assertEquals("mockmp/gradle/libs.versions.toml", versionCatalogPath("./mockmp/"))
    }
}
