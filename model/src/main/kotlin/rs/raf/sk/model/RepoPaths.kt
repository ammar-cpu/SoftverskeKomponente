package rs.raf.sk.model


/**
 * Raspored fajlova u repozitorijumu. Sve putanje su relativne u odnosu na koren,
 * sa "/" kao separatorom.
 */
object RepoPaths {

    /** Putanja do konfiguracije repozitorijuma. */
    const val CONFIG = "config.json"

    /** Ime fajla sa opisom komponente. */
    const val METADATA = "metadata.json"

    /** Folder organizacije, npr. "rs.raf". */
    fun organizationDir(organization: String): String = organization

    /** Folder projekta, npr. "rs.raf/utils". */
    fun projectDir(organization: String, project: String): String =
        "$organization/$project"

    /** Folder jedne verzije, npr. "rs.raf/utils/2.1.0". */
    fun versionDir(id: ComponentId): String =
        "${id.organization}/${id.project}/${id.version}"

    /** Putanja do metadata.json, npr. "rs.raf/utils/2.1.0/metadata.json". */
    fun metadataPath(id: ComponentId): String =
        "${versionDir(id)}/$METADATA"

    /** Ime fajla komponente, npr. "utils-2.1.0.jar". */
    fun fileName(id: ComponentId, extension: String): String =
        "${id.project}-${id.version}.$extension"

    /** Putanja do fajla komponente, npr. "rs.raf/utils/2.1.0/utils-2.1.0.jar". */
    fun filePath(id: ComponentId, extension: String): String =
        "${versionDir(id)}/${fileName(id, extension)}"
}