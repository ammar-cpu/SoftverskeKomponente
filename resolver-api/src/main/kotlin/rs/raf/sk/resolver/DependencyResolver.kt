package rs.raf.sk.resolver

import rs.raf.sk.model.ComponentId
import rs.raf.sk.model.ComponentNotFoundException
import rs.raf.sk.model.Metadata
import rs.raf.sk.model.RepoConfig
import rs.raf.sk.model.RepoPaths
import rs.raf.sk.model.RepositoryException
import rs.raf.sk.model.ValidationException
import rs.raf.sk.model.parseConfig
import rs.raf.sk.model.parseMetadata
import java.io.File
import java.io.IOException
import java.util.ServiceLoader

/**
 * Osnova za sve drajvere za preuzimanje (local, network, gdrive).
 *
 * Drajver se ne pravi direktno, nego preko [load], koji ga pronalazi kroz [ServiceLoader].
 * Zato svaki drajver mora biti `class` sa praznim konstruktorom, a lokacija repozitorijuma
 * se zadaje kasnije, pozivom [connect].
 */
abstract class DependencyResolver {

    /** Ime drajvera, npr. "local", "network" ili "gdrive". */
    abstract val type: String

    /** Pravilo za izbor verzije kada se ista komponenta pojavi u više verzija. Mora se postaviti pre [resolve]. */
    lateinit var strategy: ConflictStrategy

    /** Skladište repozitorijuma na koji je drajver povezan. Postavlja ga [connect]. */
    protected lateinit var storage: ResolverStorage
        private set

    /**
     * Pravi skladište za repozitorijum na datoj lokaciji.
     * Ovo je jedini deo koji se razlikuje od drajvera do drajvera.
     *
     * @param location lokacija repozitorijuma, u obliku koji zavisi od tipa (putanja, host:port, ime foldera)
     * @return skladište preko kog se čita repozitorijum
     * @throws RepositoryException ako lokacija nije ispravna ili nije dostupna
     */
    protected abstract fun createStorage(location: String): ResolverStorage

    /**
     * Povezuje drajver sa repozitorijumom na datoj lokaciji.
     *
     * @param location lokacija repozitorijuma, u obliku koji zavisi od tipa (putanja, host:port, ime foldera)
     * @throws RepositoryException ako lokacija nije ispravna ili nije dostupna
     */
    fun connect(location: String) {
        storage = createStorage(location)
    }

    /**
     * Čita i tumači config.json repozitorijuma na koji je drajver povezan.
     *
     * @return pravila repozitorijuma
     * @throws RepositoryException ako config.json ne postoji, ne može da se pročita ili nije ispravan
     */
    fun readConfig(): RepoConfig =
        parseConfig(storage.read(RepoPaths.CONFIG).decodeToString())

    /**
     * Obilazi stablo zavisnosti po širini (BFS) i vraća sve pronađene verzije, bez rešavanja konflikata.
     * Prati samo zavisnosti koje nisu optional i čiji je scope tranzitivan po config.json.
     * Svaka konkretna verzija se obilazi samo jednom, pa ciklus ne pravi beskonačnu petlju.
     *
     * @param roots komponente sa spiska za preuzimanje; imaju dubinu 1
     * @return pronađene verzije grupisane po ključu "organizacija:projekat", redom nailaska
     * @throws rs.raf.sk.model.ComponentNotFoundException ako za neku komponentu ne postoji metadata.json
     * @throws ValidationException ako config.json ili neki metadata.json nije ispravan
     * @throws RepositoryException ako repozitorijum ne može da se pročita
     */
    fun collectCandidates(roots: List<ComponentId>): Map<String, List<Candidate>> {
        val config = readConfig()
        val result = mutableMapOf<String, MutableList<Candidate>>()
        val visited = mutableSetOf<ComponentId>()
        val queue = ArrayDeque<Candidate>()
        var order = 0

        for (id in roots) {
            if (visited.add(id)) {
                queue.addLast(Candidate(id, 1, order++))
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.getOrPut(projectKey(current.id)) { mutableListOf() }.add(current)

            for (dependency in readMetadata(current.id).dependencies) {
                if (dependency.optional || !config.isTransitive(dependency.scope)) {
                    continue
                }
                if (visited.add(dependency.id)) {
                    queue.addLast(Candidate(dependency.id, current.depth + 1, order++))
                }
            }
        }
        return result
    }

    private fun readMetadata(id: ComponentId): Metadata {
        val path = RepoPaths.metadataPath(id)
        if (!storage.exists(path)) {
            throw ComponentNotFoundException("Component not found: $id")
        }
        return parseMetadata(storage.read(path).decodeToString())
    }

    private fun projectKey(id: ComponentId): String = "${id.organization}:${id.project}"

    /**
     * Pronalazi sve komponente koje treba preuzeti: zadate komponente i njihove tranzitivne zavisnosti.
     *
     * Konflikte rešava [strategy]. Posle izbora pobednika stablo se obilazi još jednom, samo kroz
     * pobedničke verzije, da se ne bi preuzele zavisnosti koje je tražila samo verzija koja je izgubila.
     *
     * @param roots komponente sa spiska za preuzimanje
     * @return komponente koje treba preuzeti, svaka u tačno jednoj verziji, redom nailaska
     * @throws rs.raf.sk.model.ComponentNotFoundException ako neka komponenta ne postoji u repozitorijumu
     * @throws ValidationException ako config.json ili neki metadata.json nije ispravan
     * @throws RepositoryException ako repozitorijum ne može da se pročita
     * @throws UninitializedPropertyAccessException ako [strategy] nije postavljena
     */
    fun resolve(roots: List<ComponentId>): List<ComponentId> {
        val config = readConfig()
        val winners = collectCandidates(roots).mapValues { (_, candidates) -> strategy.choose(candidates).id }

        val result = mutableListOf<ComponentId>()
        val visited = mutableSetOf<ComponentId>()
        val queue = ArrayDeque<ComponentId>()

        for (id in roots) {
            val winner = winners.getValue(projectKey(id))
            if (visited.add(winner)) {
                queue.addLast(winner)
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)

            for (dependency in readMetadata(current).dependencies) {
                if (dependency.optional || !config.isTransitive(dependency.scope)) {
                    continue
                }
                val winner = winners.getValue(projectKey(dependency.id))
                if (visited.add(winner)) {
                    queue.addLast(winner)
                }
            }
        }
        return result
    }

    /**
     * Preuzima fajlove zadatih komponenti i njihovih zavisnosti u odredišni folder.
     *
     * @param roots komponente sa spiska za preuzimanje
     * @param targetDir folder u koji se fajlovi upisuju
     * @throws rs.raf.sk.model.ComponentNotFoundException ako neka komponenta ne postoji u repozitorijumu
     * @throws RepositoryException ako fajl ne može da se pročita iz repozitorijuma
     */
    fun download(roots: List<ComponentId>, targetDir: File): Unit = TODO()

    companion object {
        /**
         * Pronalazi drajver sa datim imenom među drajverima na classpath-u.
         *
         * @param type ime drajvera, npr. "local", "network" ili "gdrive"
         * @return nov, još nepovezan drajver
         * @throws RepositoryException ako nijedan drajver na classpath-u nema to ime
         */
        @JvmStatic
        fun load(type: String): DependencyResolver {
            val drivers = ServiceLoader.load(DependencyResolver::class.java)

            return drivers.firstOrNull { it.type == type }
                ?: throw RepositoryException("No resolver found for type: $type")
        }

        /**
         * Vraća imena svih drajvera koji su na classpath-u.
         *
         * @return imena drajvera, npr. ["local", "network"]; prazna lista ako nijedan nije pronađen
         */
        @JvmStatic
        fun availableTypes(): List<String> =
            ServiceLoader.load(DependencyResolver::class.java).map { it.type }

        /**
         * Čita spisak komponenti za preuzimanje sa lokalnog diska.
         * U svakoj liniji je jedna oznaka "organizacija:projekat:verzija". Razmaci na početku i
         * kraju linije se ignorišu, a prazne linije i linije koje počinju sa "#" se preskaču.
         *
         * @param file fajl sa spiskom
         * @return komponente sa spiska, redom kojim su navedene
         * @throws ValidationException ako neka linija nije ispravna oznaka; poruka sadrži broj linije
         * @throws RepositoryException ako fajl ne postoji ili ne može da se pročita
         */
        @JvmStatic
        fun readDependencyFile(file: File): List<ComponentId> {
            val lines = try {
                file.readLines()
            } catch (e: IOException) {
                throw RepositoryException("Cannot read dependency file: ${file.path}", e)
            }

            val ids = mutableListOf<ComponentId>()
            for ((index, rawLine) in lines.withIndex()) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) {
                    continue
                }

                try {
                    ids.add(ComponentId.parse(line))
                } catch (e: ValidationException) {
                    throw ValidationException("${file.path}, line ${index + 1}: ${e.message}", e)
                }
            }
            return ids
        }
    }
}
