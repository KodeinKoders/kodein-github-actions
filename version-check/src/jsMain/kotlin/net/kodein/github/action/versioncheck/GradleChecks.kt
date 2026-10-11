package net.kodein.github.action.versioncheck


internal const val ROOT_BUILD = "build.gradle.kts"

/** [rootBuild] is the path of the root build script, which is only used in the problems. */
internal fun checkRootBuild(text: String, expectedVersion: String?, rootBuild: String = ROOT_BUILD): List<Problem> {
    val script = KotlinScript(text)

    val blocks = script.topLevelBlocks("allprojects")
    if (blocks.isEmpty()) {
        return listOf(Problem("$rootBuild has no \"allprojects { }\" block.", rootBuild))
    }

    val assignments = blocks.flatMap { script.versionAssignments(it) }
    if (assignments.isEmpty()) {
        return listOf(Problem("The \"allprojects { }\" block of $rootBuild has no \"version = ...\" assignment.", rootBuild))
    }

    return buildList {
        for (extra in assignments.drop(1)) {
            add(Problem("\"version\" is assigned more than once in the \"allprojects { }\" block.", rootBuild, extra.line))
        }
        val assignment = assignments.first()
        when {
            assignment.value == null ->
                add(Problem("\"version = ${assignment.expression}\" must be a plain string literal, to be checked.", rootBuild, assignment.line))
            expectedVersion != null && assignment.value != expectedVersion ->
                add(Problem("Version is \"${assignment.value}\" in $rootBuild, so the release must be named \"${assignment.value}\".", rootBuild, assignment.line))
        }
    }
}

/** Only the root build sets the version; any other `build.gradle.kts` doing so would fight with it. */
internal fun checkOtherBuild(path: String, text: String, rootBuild: String = ROOT_BUILD): List<Problem> =
    KotlinScript(text).versionAssignments().map {
        Problem("Only the root $rootBuild may assign \"version\".", path, it.line)
    }
