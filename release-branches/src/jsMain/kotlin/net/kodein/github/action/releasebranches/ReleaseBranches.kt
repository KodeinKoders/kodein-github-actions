package net.kodein.github.action.releasebranches


internal interface Branches {
    suspend fun exists(name: String): Boolean
    suspend fun create(name: String, commit: String)
    suspend fun reset(name: String, commit: String)
}

internal sealed interface Outcome {
    data object InvalidName : Outcome
    data class PreRelease(val tag: String) : Outcome
    data class Created(val branch: String) : Outcome
    data class Reset(val branch: String) : Outcome
}

/** Points `release/v{Major}.{Minor}` at [commit] for a final release, creating the branch if need be. */
internal suspend fun updateReleaseBranch(releaseName: String, commit: String, branches: Branches): Outcome {
    val version = parseReleaseName(releaseName) ?: return Outcome.InvalidName
    // A pre-release is not the branch's release line yet.
    version.tag?.let { return Outcome.PreRelease(it) }

    val branch = "release/v${version.major}.${version.minor}"
    return if (branches.exists(branch)) {
        branches.reset(branch, commit)
        Outcome.Reset(branch)
    } else {
        branches.create(branch, commit)
        Outcome.Created(branch)
    }
}
