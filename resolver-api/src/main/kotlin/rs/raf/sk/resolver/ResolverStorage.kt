package rs.raf.sk.resolver

/**
 * Čitanje iz repozitorijuma, bez obzira na to gde se on nalazi (disk ili Google Drive).
 *
 * Sve putanje su relativne u odnosu na koren repozitorijuma, sa "/" kao separatorom.
 * Prazan string je koren. Upisa nema, jer preuzimanje nikad ne menja repozitorijum.
 */
interface ResolverStorage {

    /** Da li na putanji postoji fajl ili folder. */
    fun exists(path: String): Boolean

    /**
     * Vraća sadržaj fajla.
     *
     * @throws rs.raf.sk.model.RepositoryException ako fajl ne postoji ili ne može da se pročita
     */
    fun read(path: String): ByteArray

    /**
     * Vraća imena fajlova i foldera direktno u datom folderu, sortirana.
     * Ako folder ne postoji, vraća praznu listu.
     */
    fun list(path: String): List<String>
}
