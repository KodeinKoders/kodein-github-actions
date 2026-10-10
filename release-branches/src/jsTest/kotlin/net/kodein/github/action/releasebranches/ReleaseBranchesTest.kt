package net.kodein.github.action.releasebranches

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals


class ReleaseBranchesTest {

    private class FakeBranches(private val existing: Set<String> = emptySet()) : Branches {
        val calls = mutableListOf<String>()

        override suspend fun exists(name: String): Boolean = (name in existing).also { calls += "exists $name" }
        override suspend fun create(name: String, commit: String) { calls += "create $name $commit" }
        override suspend fun reset(name: String, commit: String) { calls += "reset $name $commit" }
    }

    @Test
    fun invalidNameDoesNothing() = runTest {
        val branches = FakeBranches()
        assertEquals(Outcome.InvalidName, updateReleaseBranch("1.2.3", "abc", branches))
        assertEquals(emptyList(), branches.calls)
    }

    @Test
    fun preReleaseDoesNothing() = runTest {
        val branches = FakeBranches(existing = setOf("release/v1.2"))
        assertEquals(Outcome.PreRelease("RC1"), updateReleaseBranch("v1.2.3-RC1", "abc", branches))
        assertEquals(emptyList(), branches.calls)
    }

    @Test
    fun missingBranchIsCreated() = runTest {
        val branches = FakeBranches()
        assertEquals(Outcome.Created("release/v1.2"), updateReleaseBranch("v1.2.3", "abc", branches))
        assertEquals(listOf("exists release/v1.2", "create release/v1.2 abc"), branches.calls)
    }

    @Test
    fun existingBranchIsReset() = runTest {
        val branches = FakeBranches(existing = setOf("release/v1.2"))
        assertEquals(Outcome.Reset("release/v1.2"), updateReleaseBranch("v1.2.4", "def", branches))
        assertEquals(listOf("exists release/v1.2", "reset release/v1.2 def"), branches.calls)
    }

    @Test
    fun branchNameUsesMajorAndMinorOnly() = runTest {
        val branches = FakeBranches(existing = setOf("release/v1.2"))
        assertEquals(Outcome.Created("release/v1.12"), updateReleaseBranch("v1.12.0", "abc", branches))
        assertEquals(listOf("exists release/v1.12", "create release/v1.12 abc"), branches.calls)
    }

    @Test
    fun releasePrefixCanBeSkipped() = runTest {
        val branches = FakeBranches()
        assertEquals(Outcome.Created("v1.2"), updateReleaseBranch("v1.2.3", "abc", branches, skipReleasePrefix = true))
        assertEquals(listOf("exists v1.2", "create v1.2 abc"), branches.calls)
    }

    @Test
    fun existingBranchWithoutPrefixIsReset() = runTest {
        val branches = FakeBranches(existing = setOf("v1.2"))
        assertEquals(Outcome.Reset("v1.2"), updateReleaseBranch("v1.2.4", "def", branches, skipReleasePrefix = true))
        assertEquals(listOf("exists v1.2", "reset v1.2 def"), branches.calls)
    }
}
