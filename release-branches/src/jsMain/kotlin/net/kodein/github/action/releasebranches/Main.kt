package net.kodein.github.action.releasebranches

import actions.core.InputOptions
import actions.core.getInput
import actions.core.info
import actions.core.setFailed
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


private suspend fun runAction() {
    val releaseName = getInput("releaseName", InputOptions(required = true))
    val commit = getInput("commit", InputOptions(required = true))
    val token = getInput("token", InputOptions(required = true))

    when (val outcome = updateReleaseBranch(releaseName, commit, OctokitBranches(token))) {
        Outcome.InvalidName ->
            setFailed("Release name \"$releaseName\" must be \"v{Major}.{Minor}.{Patch}\", optionally followed by \"-{Tag}\".")
        is Outcome.PreRelease ->
            info("Release \"$releaseName\" has the tag \"${outcome.tag}\": release branches are not updated for pre-releases.")
        is Outcome.Created -> info("Created ${outcome.branch} at $commit.")
        is Outcome.Reset -> info("Reset ${outcome.branch} to $commit.")
    }
}

/** The action's entry point: bundled and called from the generated entry file, as a library has no `main`. */
@JsExport
public fun run() {
    MainScope().launch {
        try {
            runAction()
        } catch (e: Throwable) {
            setFailed("release-branches failed: ${e.message}")
        }
    }
}
