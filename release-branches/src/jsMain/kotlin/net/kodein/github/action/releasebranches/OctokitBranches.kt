package net.kodein.github.action.releasebranches

import actions.github.context
import actions.github.getOctokit
import js.promise.Promise
import js.promise.await


// The wrapper does not type octokit: its client, its methods and the Promises they return are all `dynamic`.
private fun jsObject(vararg properties: Pair<String, Any>): dynamic {
    val obj = js("{}")
    for ((key, value) in properties) obj[key] = value
    return obj
}

private suspend fun awaitPromise(promise: dynamic) {
    promise.unsafeCast<Promise<*>>().await()
}

internal class OctokitBranches(token: String) : Branches {

    // Lazy: a pre-release never reaches the API, and `context.repo` throws outside of a GitHub workflow.
    private val git by lazy { getOctokit(token).rest.git }
    private val owner by lazy { context.repo.owner }
    private val repo by lazy { context.repo.repo }

    override suspend fun exists(name: String): Boolean =
        try {
            // Unlike `matching-refs`, `getRef` matches the exact ref: `release/v1.2` is not `release/v1.20`.
            awaitPromise(git.getRef(jsObject("owner" to owner, "repo" to repo, "ref" to "heads/$name")))
            true
        } catch (e: Throwable) {
            if (e.asDynamic().status == 404) false else throw e
        }

    override suspend fun create(name: String, commit: String) {
        awaitPromise(git.createRef(jsObject("owner" to owner, "repo" to repo, "ref" to "refs/heads/$name", "sha" to commit)))
    }

    // `force` is what makes this a hard reset: without it, a branch can only be fast-forwarded.
    override suspend fun reset(name: String, commit: String) {
        awaitPromise(git.updateRef(jsObject("owner" to owner, "repo" to repo, "ref" to "heads/$name", "sha" to commit, "force" to true)))
    }
}
