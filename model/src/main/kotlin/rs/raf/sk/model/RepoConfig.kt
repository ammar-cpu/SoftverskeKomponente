package rs.raf.sk.model

import kotlinx.serialization.Serializable

/**
 * Pravila repozitorijuma, kako su zapisana u config.json.
 */
@Serializable
data class RepoConfig (
    val allowedExtensions: List<String>,
    val scopes: Map<String, Boolean>
){
    /** Da li je ekstenzija dozvoljena (ne razlikuje velika i mala slova)*/
    fun isExtensionAllowed(extension: String): Boolean =
        allowedExtensions.any{ it.equals(extension, ignoreCase = true) }

    /** Da li scope postoji u ovom repozitorijumu. */
    fun hasScope(scope: String): Boolean = scope in scopes

    /** Da li se zavisnosti ovog scope-a povlace tranzitivno. */
    fun isTransitive(scope: String): Boolean = scopes[scope] ?: false
}