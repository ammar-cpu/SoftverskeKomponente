package rs.raf.sk.model

import kotlinx.serialization.Serializable

/**
 * Jedna zavisnost komponente, kako je zapisana u metadata.json
 */
@Serializable
data class Dependency (
    val organization: String,
    val project: String,
    val version: String,
    val scope: String,
    val optional: Boolean = false
) {
    /** Oznaka komponente od koje se zavisi. */
    val id: ComponentId
        get() = ComponentId(organization, project, version)
}