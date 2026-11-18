package rs.raf.sk.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val json = Json { prettyPrint = true }

/**
 * Čita config.json iz teksta.
 *
 * @param text sadržaj config.json
 * @return pravila repozitorijuma
 * @throws ValidationException ako tekst nije ispravan JSON ili mu nedostaje neko polje
 */
fun parseConfig(text: String): RepoConfig {
    try {
        return json.decodeFromString(RepoConfig.serializer(), text)
    } catch (e: SerializationException) {
        throw ValidationException("Invalid ${RepoPaths.CONFIG}: ${e.message}", e)
    }
}

/**
 * Pretvara pravila repozitorijuma u tekst za config.json.
 *
 * @param config pravila repozitorijuma
 * @return formatiran JSON
 */
fun configToJson(config: RepoConfig): String =
    json.encodeToString(RepoConfig.serializer(), config)

/**
 * Čita metadata.json iz teksta.
 *
 * @param text sadržaj metadata.json
 * @return opis komponente
 * @throws ValidationException ako tekst nije ispravan JSON ili mu nedostaje neko polje
 */
fun parseMetadata(text: String): Metadata {
    try {
        return json.decodeFromString(Metadata.serializer(), text)
    } catch (e: SerializationException) {
        throw ValidationException("Invalid ${RepoPaths.METADATA}: ${e.message}", e)
    }
}

/**
 * Pretvara opis komponente u tekst za metadata.json.
 *
 * @param metadata opis komponente
 * @return formatiran JSON
 */
fun metadataToJson(metadata: Metadata): String =
    json.encodeToString(Metadata.serializer(), metadata)
