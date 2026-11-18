package rs.raf.sk.local.resolver

import rs.raf.sk.resolver.DependencyResolver
import rs.raf.sk.resolver.ResolverStorage
import java.nio.file.Path

/**
 * Drajver za repozitorijum koji je folder na lokalnom disku.
 * Lokacija je putanja do korenskog foldera, npr. "./moj-repo".
 */
class LocalResolver : DependencyResolver() {

    override val type: String = "local"

    override fun createStorage(location: String): ResolverStorage =
        LocalResolverStorage(Path.of(location))
}
