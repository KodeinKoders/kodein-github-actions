package net.kodein.github.action.versioncheck

import actions.core.AnnotationProperties
import actions.core.InputOptions
import actions.core.error
import actions.core.getInput
import actions.core.info
import actions.core.setFailed
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


private suspend fun checkAll(releaseName: String, tagName: String, checkGradle: Boolean, gradleBuildPath: String): List<Problem> = buildList {
    info("Checking the release name")
    addAll(checkReleaseName(releaseName))
    info("Checking the tag name")
    addAll(checkTagName(tagName, releaseName))
    val version = parseReleaseVersion(releaseName)

    // Without a valid version, there is nothing to compare the Gradle projects to: the release name is already reported.
    if (checkGradle && version != null) {
        info("Checking the version of the Gradle projects")
        addAll(checkGradleVersions(gradleBuildPath, version))
    }

    if (exists(ANTORA_FILE)) {
        info("Checking $ANTORA_FILE")
        val catalogPath = versionCatalogPath(gradleBuildPath)
        val catalog = if (exists(catalogPath)) parseCatalogVersions(readText(catalogPath)) else emptyMap()
        addAll(checkAntora(readText(ANTORA_FILE), catalog, version, catalogPath))
    }
}

/** The action's entry point: bundled and called from the generated entry file, as a library has no `main`. */
@JsExport
public fun run() {
    MainScope().launch {
        try {
            val problems = checkAll(
                releaseName = getInput("release-name", InputOptions(required = true)),
                tagName = getInput("tag-name"),
                checkGradle = getInput("check-gradle") == "true",
                gradleBuildPath = getInput("gradle-build-path"),
            )
            for (problem in problems) {
                error(
                    problem.message,
                    AnnotationProperties(title = "version-check", file = problem.file, startLine = problem.line),
                )
            }
            if (problems.isEmpty()) info("All version checks passed") else setFailed("${problems.size} version check(s) failed")
        } catch (e: Throwable) {
            setFailed("version-check crashed: ${e.message}")
        }
    }
}
