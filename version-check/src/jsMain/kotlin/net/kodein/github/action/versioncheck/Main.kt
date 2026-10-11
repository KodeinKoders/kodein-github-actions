package net.kodein.github.action.versioncheck

import actions.core.AnnotationProperties
import actions.core.InputOptions
import actions.core.error
import actions.core.getInput
import actions.core.info
import actions.core.setFailed


private fun checkAll(releaseName: String, tagName: String, checkGradle: Boolean, gradleDirectory: String): List<Problem> = buildList {
    info("Checking the release name")
    addAll(checkReleaseName(releaseName))
    info("Checking the tag name")
    addAll(checkTagName(tagName, releaseName))
    val version = parseReleaseVersion(releaseName)

    if (checkGradle) {
        val rootBuild = rootBuildPath(gradleDirectory)
        info("Checking $rootBuild")
        if (!exists(rootBuild)) {
            add(Problem("$rootBuild does not exist.", rootBuild))
        } else {
            addAll(checkRootBuild(readText(rootBuild), version, rootBuild))

            info("Checking that no other build.gradle.kts sets a version")
            for (path in findOtherBuildScripts(rootBuild)) {
                addAll(checkOtherBuild(path, readText(path), rootBuild))
            }
        }
    }

    if (exists(ANTORA_FILE)) {
        info("Checking $ANTORA_FILE")
        val catalogPath = versionCatalogPath(gradleDirectory)
        val catalog = if (exists(catalogPath)) parseCatalogVersions(readText(catalogPath)) else emptyMap()
        addAll(checkAntora(readText(ANTORA_FILE), catalog, version, catalogPath))
    }
}

/** The action's entry point: bundled and called from the generated entry file, as a library has no `main`. */
@JsExport
public fun run() {
    try {
        val problems = checkAll(
            releaseName = getInput("release-name", InputOptions(required = true)),
            tagName = getInput("tag-name"),
            checkGradle = getInput("check-gradle") == "true",
            gradleDirectory = getInput("gradle-directory"),
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
