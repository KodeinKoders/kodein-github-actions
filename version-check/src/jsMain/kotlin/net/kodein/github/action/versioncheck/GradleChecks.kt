package net.kodein.github.action.versioncheck


internal const val ROOT_BUILD = "build.gradle.kts"

internal fun checkRootBuild(text: String, expectedVersion: String?): List<Problem> {
    val script = KotlinScript(text)

    val blocks = script.topLevelBlocks("allprojects")
    if (blocks.isEmpty()) {
        return listOf(Problem("$ROOT_BUILD has no \"allprojects { }\" block.", ROOT_BUILD))
    }

    val assignments = blocks.flatMap { script.versionAssignments(it) }
    if (assignments.isEmpty()) {
        return listOf(Problem("The \"allprojects { }\" block of $ROOT_BUILD has no \"version = ...\" assignment.", ROOT_BUILD))
    }

    return buildList {
        for (extra in assignments.drop(1)) {
            add(Problem("\"version\" is assigned more than once in the \"allprojects { }\" block.", ROOT_BUILD, extra.line))
        }
        val assignment = assignments.first()
        when {
            assignment.value == null ->
                add(Problem("\"version = ${assignment.expression}\" must be a plain string literal, to be checked.", ROOT_BUILD, assignment.line))
            expectedVersion != null && assignment.value != expectedVersion ->
                add(Problem("Version is \"${assignment.value}\" in $ROOT_BUILD, so the release must be named \"${assignment.value}\".", ROOT_BUILD, assignment.line))
        }
    }
}

/** Only the root build sets the version; any other `build.gradle.kts` doing so would fight with it. */
internal fun checkOtherBuild(path: String, text: String): List<Problem> =
    KotlinScript(text).versionAssignments().map {
        Problem("Only the root $ROOT_BUILD may assign \"version\".", path, it.line)
    }
