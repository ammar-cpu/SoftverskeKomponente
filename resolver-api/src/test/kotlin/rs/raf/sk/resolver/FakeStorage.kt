package rs.raf.sk.resolver

import rs.raf.sk.model.RepositoryException

/** Skladište koje drži fajlove u memoriji, bez diska. Folderi postoje samo kao delovi putanja. */
class FakeStorage(private val files: Map<String, ByteArray>) : ResolverStorage {

    override fun exists(path: String): Boolean =
        path.isEmpty() || path in files || files.keys.any { it.startsWith("$path/") }

    override fun read(path: String): ByteArray =
        files[path] ?: throw RepositoryException("Cannot read file: $path")

    override fun list(path: String): List<String> {
        val prefix = if (path.isEmpty()) "" else "$path/"
        return files.keys
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix).substringBefore("/") }
            .distinct()
            .sorted()
    }
}
