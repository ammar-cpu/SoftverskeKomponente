package rs.raf.sk.publisher

/**
 * Skladiste u kom se nalazi repozitorijum. Implementira ga svaki drajver
 * (lokalni, mrezni, Google Drive). Sve putanje su relativne u odnosu na koren
 * repozitorijuma, sa "/" kao separatorom; prazan string je sam koren.
 */
interface PublisherStorage {

    /** Da li fajl ili folder na putanji postoji. */
    fun exists(path: String): Boolean

    /** Cita sadrzaj fajla. */
    fun read(path: String): ByteArray

    /** Upisuje sadrzaj u fajl. Folderi iznad moraju vec da postoje. */
    fun write(path: String, data: ByteArray)

    /** Imena stavki u folderu, sortirana. Prazna lista ako folder ne postoji. */
    fun list(path: String): List<String>

    /** Pravi folder, zajedno sa svim folderima iznad koji fale. */
    fun createDir(path: String)
}