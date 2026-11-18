package rs.raf.sk.resolver

import rs.raf.sk.model.ComponentId
import rs.raf.sk.model.Dependency
import rs.raf.sk.model.Metadata
import rs.raf.sk.model.RepoConfig
import rs.raf.sk.model.RepoPaths
import rs.raf.sk.model.configToJson
import rs.raf.sk.model.metadataToJson

/** Drajver za testove: umesto prave lokacije uvek koristi zadato skladište. */
class TestResolver(private val fakeStorage: ResolverStorage) : DependencyResolver() {

    override val type: String = "test"

    override fun createStorage(location: String): ResolverStorage = fakeStorage
}

/**
 * Sastavlja repozitorijum u memoriji. Podrazumevani config je isti kao u examples/test-repo.
 *
 * Primer:
 * ```
 * TestRepo()
 *     .component("rs.raf:app:1.0.0", dep("rs.raf:utils:1.0.0"))
 *     .component("rs.raf:utils:1.0.0")
 *     .resolver(NearestWinsStrategy)
 * ```
 */
class TestRepo(
    scopes: Map<String, Boolean> = mapOf("compile" to true, "runtime" to true, "test" to false, "provided" to false)
) {
    private val files = mutableMapOf<String, ByteArray>()

    init {
        files[RepoPaths.CONFIG] = configToJson(RepoConfig(listOf("jar"), scopes)).toByteArray()
    }

    fun component(id: String, vararg dependencies: Dependency): TestRepo {
        val componentId = ComponentId.parse(id)
        val metadata = Metadata(
            componentId.organization,
            componentId.project,
            componentId.version,
            RepoPaths.fileName(componentId, "jar"),
            dependencies.toList()
        )
        files[RepoPaths.metadataPath(componentId)] = metadataToJson(metadata).toByteArray()
        return this
    }

    fun resolver(strategy: ConflictStrategy): TestResolver {
        val resolver = TestResolver(FakeStorage(files))
        resolver.connect("memory")
        resolver.strategy = strategy
        return resolver
    }
}

fun dep(id: String, scope: String = "compile", optional: Boolean = false): Dependency {
    val componentId = ComponentId.parse(id)
    return Dependency(componentId.organization, componentId.project, componentId.version, scope, optional)
}

fun ids(vararg ids: String): List<ComponentId> = ids.map { ComponentId.parse(it) }
