package rs.raf.sk.publisher

import rs.raf.sk.model.ComponentId
import rs.raf.sk.model.ComponentNotFoundException
import rs.raf.sk.model.Dependency
import rs.raf.sk.model.Metadata
import rs.raf.sk.model.RepoConfig
import rs.raf.sk.model.RepoPaths
import rs.raf.sk.model.RepositoryException
import rs.raf.sk.model.ValidationException
import rs.raf.sk.model.compareVersions
import rs.raf.sk.model.configToJson
import rs.raf.sk.model.isValidVersion
import rs.raf.sk.model.metadataToJson
import rs.raf.sk.model.parseConfig
import rs.raf.sk.model.parseMetadata
import java.util.ServiceLoader


/**
 * Biblioteka za pravljenje repozitorijuma i postavljanje komponenti.
 * Osnova za sve drajvere (local, network, gdrive).
 *
 * Drajver se ne pravi direktno, nego preko [load], koji ga pronalazi kroz ServiceLoader.
 * Lokacija repozitorijuma se zadaje kasnije, pozivom [connect].
 */
abstract class Publisher {

    /** Ime drajvera, npr. "local", "network" ili "gdrive". */
    abstract val type: String

    /** Skladište repozitorijuma na koji je drajver povezan. Postavlja ga [connect]. */
    protected lateinit var storage: PublisherStorage
        private set

    /**
     * Pravi skladište za repozitorijum na datoj lokaciji.
     * Ovo je jedini deo koji se razlikuje od drajvera do drajvera.
     */
    protected abstract fun createStorage(location: String): PublisherStorage

    // REPOZITORIJUM I KONFIGURACIJA

    /**
     * Povezuje drajver sa repozitorijumom na datoj lokaciji.
     *
     * @param location lokacija repozitorijuma (putanja, host:port, ime foldera na Drive-u)
     */
    fun connect(location: String) {
        storage = createStorage(location)
    }

    /**
     * Pravi novi repozitorijum na datoj lokaciji sa datom konfiguracijom.
     *
     * @throws ValidationException ako konfiguracija nije ispravna ili repozitorijum već postoji
     *
     */
    fun init(config: RepoConfig) {
        validateConfig(config)
        if (storage.exists(RepoPaths.CONFIG)) {
            throw ValidationException("Na ovoj lokaciji vec postoji repozitorijum")
        }
        storage.createDir("")
        writeConfig(config)
    }


    /**
     * Vraća trenutnu konfiguraciju repozitorijuma.
     *
     * @throws RepositoryException ako na lokaciji nema repozitorijuma
     */
    fun getConfig() : RepoConfig {
        requireRepository()
        return parseConfig(storage.read(RepoPaths.CONFIG).decodeToString())
    }

    /**
     * Menja konfiguraciju. Nova pravila vaze za komponente postavljene posle izmena.
     *
     * @throws ValidationException ako konfiguracija nije ispravna
     * @throws RepositoryException ako na lokaciji nema repozitorijuma
     */
    fun setConfig(config: RepoConfig) {
        requireRepository()
        validateConfig(config)
        writeConfig(config)
    }

    // POSTAVLJANJE

    /**
     * Postavlja komponentu u repozitorijum.
     *
     * @param id oznaka komponente
     * @param extension ekstenzija fajla, npr. "jar"
     * @param content sadržaj fajla komponente
     * @param dependencies zavisnosti komponente
     * @return opis postavljene komponente
     * @throws ValidationException ako komponenta ne prolazi neku proveru
     */
    fun deploy(
        id: ComponentId,
        extension: String,
        content: ByteArray,
        dependencies: List<Dependency> = emptyList(),
    ): Metadata {
        val config = getConfig()
        val ext = extension.removePrefix(".").lowercase()

        if (!isValidVersion(id.version)) {
            throw ValidationException("Neispravna verzija '${id.version}': dozvoljeni su samo brojevi razdvojeni tačkama")
        }
        if(!config.isExtensionAllowed(ext)) {
            throw ValidationException("Ekstenzija '$ext' nije dozvoljena, dozvoljene su: ${config.allowedExtensions.joinToString()}")
        }
        if(storage.exists(RepoPaths.metadataPath(id))) {
            throw ValidationException("Komponenta $id vec postoji")
        }

        checkDependencies(id, dependencies, config)

        val metadata = Metadata(
            organization =  id.organization,
            project = id.project,
            version = id.version,
            file = RepoPaths.fileName(id, ext),
            dependencies = dependencies
        )

        storage.createDir(RepoPaths.versionDir(id))
        storage.write(RepoPaths.filePath(id, ext), content)
        storage.write(RepoPaths.metadataPath(id), metadataToJson(metadata).encodeToByteArray())
        return metadata
    }

    private fun checkDependencies(
        id: ComponentId,
        dependencies: List<Dependency>,
        config: RepoConfig
    ) {
        val seen = mutableSetOf<String>()

        for (dep in dependencies) {
            val depId = dep.id  // baca ValidationException ako neki podatak fali

            if(depId.organization == id.organization && depId.project == id.project) {
                throw ValidationException("Komponenta $id ne može da zavisi od svog projekta")
            }

            val key = "${depId.organization}:${depId.project}"
            if (!seen.add(key)) {
                throw ValidationException("Zavisnost $key je navedena više puta")
            }
            if (!config.hasScope(dep.scope)) {
                throw ValidationException("Scope '${dep.scope}' (zavisnost $depId) ne postoji, dozvoljeni su: ${config.scopes.keys.joinToString()}")
            }
            if (!storage.exists(RepoPaths.metadataPath(depId))) {
                throw ValidationException("Zavisnost $depId ne postoji u repozitorijumu")
            }
        }
    }

    // PRETRAGA

    /** Sve organizacije u repozitorijumu. */
    fun listOrganizations(): List<String> =
        storage.list("").filter { it != RepoPaths.CONFIG }

    /** Svi projekti jedne organizacije. Prazna lista ako je nema. */
    fun listProjects(organization: String): List<String> =
        storage.list(RepoPaths.organizationDir(organization))

    /** Sve verzije projekta, od najstarije do najnovije. */
    fun listVersions(organization: String, project: String): List<String> =
        storage.list(RepoPaths.projectDir(organization, project))
            .sortedWith { a, b -> compareVersions(a, b)  }

    /**
     * Opis komponente.
     *
     * @throws ComponentNotFoundException ako komponenta ne postoji
     */
    fun getMetadata(id: ComponentId): Metadata {
        val path = RepoPaths.metadataPath(id)
        if(!storage.exists(path)) {
            throw ComponentNotFoundException("Komponenta $id ne postoji")
        }
        return parseMetadata(storage.read(path).decodeToString())
    }

    /**
     * Direktne zavisnosti komponente.
     *
     * @throws ComponentNotFoundException ako komponenta ne postoji
     */
    fun getDependencies(id: ComponentId): List<Dependency> =
        getMetadata(id).dependencies


    // POMOCNE FUNKCIJE

    private fun validateConfig(config: RepoConfig) {
        if (config.allowedExtensions.isEmpty()) {
            throw ValidationException("Mora biti dozvoljena bar jedna ekstenzija.")
        }
        if (config.allowedExtensions.any { it.isBlank() || it.startsWith(".")}) {
            throw ValidationException("Ekstenzije se pisu bez tacke, npr. \"jar\"")
        }
        if (config.scopes.isEmpty() || config.scopes.keys.any { it.isBlank()}) {
            throw ValidationException("Mora postojati bar jedan scope i nijedan ne sme biti prazan.")
        }
    }

    private fun writeConfig(config: RepoConfig) {
        storage.write(RepoPaths.CONFIG, configToJson(config).encodeToByteArray())
    }

    private fun requireRepository() {
        if (!storage.exists(RepoPaths.CONFIG)) {
            throw RepositoryException("Na ovoj lokaciji nema repozitorijuma, prvo pozovi init")
        }
    }

    companion object {
        /**
         * Pronalazi drajver sa datim imenom među drajverima na classpath-u.
         *
         * @param type ime drajvera, npr. "local", "network" ili "gdrive"
         * @return nov, još nepovezan drajver
         * @throws RepositoryException ako nijedan drajver nema to ime
         */
        @JvmStatic
        fun load(type: String): Publisher =
            ServiceLoader.load(Publisher::class.java).firstOrNull { it.type == type }
                ?: throw RepositoryException("Nije pronađen drajver tipa: $type")

        /** Imena svih drajvera na classpath-u, npr. ["local", "network"]. */
        @JvmStatic
        fun availableTypes(): List<String> =
            ServiceLoader.load(Publisher::class.java).map { it.type }
    }
}