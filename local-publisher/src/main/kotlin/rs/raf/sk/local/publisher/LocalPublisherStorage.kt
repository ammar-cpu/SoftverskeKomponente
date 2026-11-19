package rs.raf.sk.local.publisher

import rs.raf.sk.model.ComponentNotFoundException
import rs.raf.sk.model.RepositoryException
import rs.raf.sk.publisher.PublisherStorage
import java.io.File

/**
 * Drajver za repozitorijum na lokalnom disku.
 * Lokacija je putanja do korenskog foldera, npr. "./moj-repo".
 */

class LocalPublisherStorage : PublisherStorage {

    private var root: File? = null

    override fun connect(location: String) {
        root = File(location)
    }

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

    /** Od relativne putanje napravi pravi fajl unutar korena. */
    private fun resolve(path: String): File {
        val r = root ?: throw RepositoryException("Skladište nije povezano, prvo pozovi connect")
        return if (path.isEmpty()) r else File(r, path)

    }
}