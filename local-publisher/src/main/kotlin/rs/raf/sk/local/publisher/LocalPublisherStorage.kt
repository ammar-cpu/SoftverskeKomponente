package rs.raf.sk.local.publisher

import rs.raf.sk.model.ComponentNotFoundException
import rs.raf.sk.model.RepositoryException
import rs.raf.sk.publisher.PublisherStorage
import java.io.File

/**
 * Skladište za repozitorijum koji je folder na lokalnom disku.
 * Putanje se tumače relativno u odnosu na [root]; prazan string je sam koren.
 */
internal class LocalPublisherStorage(private val root: File) : PublisherStorage {

    override fun exists(path: String): Boolean = resolve(path).exists()

    override fun read(path: String): ByteArray {
        val file = resolve(path)
        if (!file.isFile){
            throw ComponentNotFoundException("Fajl ne postoji: $path")
        }
        return file.readBytes()
    }

    override fun write(path: String, data: ByteArray) {
        resolve(path).writeBytes(data)
    }

    override fun list(path: String): List<String> =
        resolve(path).listFiles()?.map { it.name }?.sorted() ?: emptyList()


    override fun createDir(path: String) {
        resolve(path).mkdirs()
    }

    private fun resolve(path: String): File =
        if (path.isEmpty()) root else File(root, path)

}