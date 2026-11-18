package rs.raf.sk.local.resolver

import rs.raf.sk.model.RepositoryException
import rs.raf.sk.resolver.ResolverStorage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Skladište za repozitorijum koji je folder na lokalnom disku.
 * Putanje se tumače relativno u odnosu na [root]; prazan string je sam koren.
 */
internal class LocalResolverStorage(private val root: Path) : ResolverStorage {

    override fun exists(path: String): Boolean = Files.exists(root.resolve(path))

    override fun read(path: String): ByteArray {
        try {
            return Files.readAllBytes(root.resolve(path))
        } catch (e: IOException) {
            throw RepositoryException("Cannot read file: $path").apply { initCause(e) }
        }
    }

    override fun list(path: String): List<String> {
        val dir = root.resolve(path)
        if (!Files.isDirectory(dir)) {
            return emptyList()
        }

        try {
            Files.list(dir).use { stream ->
                return stream.map { it.fileName.toString() }.toList().sorted()
            }
        } catch (e: IOException) {
            throw RepositoryException("Cannot list folder: $path").apply { initCause(e) }
        }
    }
}
