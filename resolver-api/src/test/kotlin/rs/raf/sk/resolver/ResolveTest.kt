package rs.raf.sk.resolver

import rs.raf.sk.model.ComponentNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResolveTest {

    // Isti graf kao examples/test-repo, plus utils:2.0.0 -> json:1.0.0
    private fun testRepoWithJson() = TestRepo()
        .component(
            "rs.raf:app:1.0.0",
            dep("rs.raf:utils:1.0.0"),
            dep("rs.raf:network:3.0.0"),
            dep("org.junit:junit:5.0.0", scope = "test"),
            dep("rs.raf:log:1.0.0", optional = true)
        )
        .component("rs.raf:network:3.0.0", dep("rs.raf:utils:2.0.0"))
        .component("rs.raf:utils:1.0.0")
        .component("rs.raf:utils:2.0.0", dep("rs.raf:json:1.0.0"))
        .component("rs.raf:json:1.0.0")
        .component("org.junit:junit:5.0.0")
        .component("rs.raf:log:1.0.0")

    @Test
    fun testAndProvidedScopesAndOptionalDependenciesAreNotPulled() {
        val resolver = TestRepo()
            .component(
                "rs.raf:app:1.0.0",
                dep("rs.raf:utils:1.0.0"),
                dep("rs.raf:runner:1.0.0", scope = "runtime"),
                dep("org.junit:junit:5.0.0", scope = "test"),
                dep("javax:servlet:4.0.0", scope = "provided"),
                dep("rs.raf:log:1.0.0", optional = true)
            )
            .component("rs.raf:utils:1.0.0")
            .component("rs.raf:runner:1.0.0")
            .component("org.junit:junit:5.0.0")
            .component("javax:servlet:4.0.0")
            .component("rs.raf:log:1.0.0")
            .resolver(NearestWinsStrategy)

        val result = resolver.resolve(ids("rs.raf:app:1.0.0"))

        assertEquals(ids("rs.raf:app:1.0.0", "rs.raf:utils:1.0.0", "rs.raf:runner:1.0.0"), result)
    }

    @Test
    fun nearestAndNewestGiveDifferentResultsOnTheSameGraph() {
        val roots = ids("rs.raf:app:1.0.0")

        val nearest = testRepoWithJson().resolver(NearestWinsStrategy).resolve(roots)
        val newest = testRepoWithJson().resolver(NewestVersionStrategy).resolve(roots)

        assertEquals(ids("rs.raf:app:1.0.0", "rs.raf:utils:1.0.0", "rs.raf:network:3.0.0"), nearest)
        assertEquals(
            ids("rs.raf:app:1.0.0", "rs.raf:utils:2.0.0", "rs.raf:network:3.0.0", "rs.raf:json:1.0.0"),
            newest
        )
    }

    @Test
    fun dependencyOfLosingVersionIsNotInResult() {
        val resolver = testRepoWithJson().resolver(NearestWinsStrategy)
        val roots = ids("rs.raf:app:1.0.0")

        // prvi prolaz nalazi json preko utils:2.0.0 ...
        assertTrue("rs.raf:json" in resolver.collectCandidates(roots))
        // ... ali utils:2.0.0 gubi od utils:1.0.0, pa drugi prolaz do json ne stiže
        assertTrue(ids("rs.raf:json:1.0.0").first() !in resolver.resolve(roots))
    }

    @Test
    fun nearestPicksFirstDiscoveredVersionOnSameDepth() {
        // lib:1.0.0 i lib:2.0.0 su obe na dubini 3; razlikuje ih samo redosled zavisnosti u app
        fun repo(first: String, second: String) = TestRepo()
            .component("rs.raf:app:1.0.0", dep(first), dep(second))
            .component("rs.raf:a:1.0.0", dep("rs.raf:lib:1.0.0"))
            .component("rs.raf:b:1.0.0", dep("rs.raf:lib:2.0.0"))
            .component("rs.raf:lib:1.0.0")
            .component("rs.raf:lib:2.0.0")
            .resolver(NearestWinsStrategy)

        val aFirst = repo("rs.raf:a:1.0.0", "rs.raf:b:1.0.0").resolve(ids("rs.raf:app:1.0.0"))
        val bFirst = repo("rs.raf:b:1.0.0", "rs.raf:a:1.0.0").resolve(ids("rs.raf:app:1.0.0"))

        assertTrue(ids("rs.raf:lib:1.0.0").first() in aFirst)
        assertTrue(ids("rs.raf:lib:2.0.0").first() in bFirst)
    }

    @Test
    fun cycleDoesNotLoopForever() {
        val resolver = TestRepo()
            .component("rs.raf:a:1.0.0", dep("rs.raf:b:1.0.0"))
            .component("rs.raf:b:1.0.0", dep("rs.raf:a:1.0.0"))
            .resolver(NearestWinsStrategy)

        val result = resolver.resolve(ids("rs.raf:a:1.0.0"))

        assertEquals(ids("rs.raf:a:1.0.0", "rs.raf:b:1.0.0"), result)
    }

    @Test
    fun missingDependencyThrowsComponentNotFound() {
        val resolver = TestRepo()
            .component("rs.raf:app:1.0.0", dep("rs.raf:missing:1.0.0"))
            .resolver(NearestWinsStrategy)

        val e = assertFailsWith<ComponentNotFoundException> {
            resolver.resolve(ids("rs.raf:app:1.0.0"))
        }
        assertTrue("rs.raf:missing:1.0.0" in e.message!!)
    }

    @Test
    fun missingRootThrowsComponentNotFound() {
        val resolver = TestRepo().resolver(NearestWinsStrategy)

        assertFailsWith<ComponentNotFoundException> {
            resolver.resolve(ids("rs.raf:app:1.0.0"))
        }
    }
}
