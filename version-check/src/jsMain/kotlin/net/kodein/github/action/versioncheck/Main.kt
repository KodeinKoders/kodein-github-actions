package net.kodein.github.action.versioncheck

import actions.core.AnnotationProperties
import actions.core.InputOptions
import actions.core.error
import actions.core.getInput
import actions.core.info
import actions.core.setFailed


private fun checkAll(releaseName: String): List<Problem> = buildList {
    info("Checking the release name")
    addAll(checkReleaseName(releaseName))
    val version = parseReleaseVersion(releaseName)

    info("Checking $ROOT_BUILD")
    if (!exists(ROOT_BUILD)) {
        add(Problem("$ROOT_BUILD does not exist.", ROOT_BUILD))
    } else {
        addAll(checkRootBuild(readText(ROOT_BUILD), version))
    }

    info("Checking that no other build.gradle.kts sets a version")
    for (path in findOtherBuildScripts()) {
        addAll(checkOtherBuild(path, readText(path)))
    }

    if (exists("doc")) {
        info("Checking $ANTORA_FILE")
        when {
            !exists(ANTORA_FILE) -> add(Problem("$ANTORA_FILE does not exist.", ANTORA_FILE))
            !exists(VERSION_CATALOG) -> add(Problem("$VERSION_CATALOG does not exist, but $ANTORA_FILE versions are checked against it.", VERSION_CATALOG))
            else -> addAll(checkAntora(readText(ANTORA_FILE), parseCatalogVersions(readText(VERSION_CATALOG))))
        }
    }
}

/** The action's entry point: bundled and called from the generated entry file, as a library has no `main`. */
@JsExport
public fun run() {
    try {
        val problems = checkAll(getInput("releaseName", InputOptions(required = true)))
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
