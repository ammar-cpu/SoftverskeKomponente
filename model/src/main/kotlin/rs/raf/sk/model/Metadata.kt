package rs.raf.sk.model

import kotlinx.serialization.Serializable

/**
 * Opis jedne komponente, kako je zapisana u metadata.json
 */
@Serializable
data class Metadata (
    val organization: String,
    val project: String,
    val version: String,
    val file: String,
    val dependencies: List<Dependency> = emptyList()
) {
    /** Oznaka ove komponente. */
    val id: ComponentId
        get() = ComponentId(organization, project, version)
}