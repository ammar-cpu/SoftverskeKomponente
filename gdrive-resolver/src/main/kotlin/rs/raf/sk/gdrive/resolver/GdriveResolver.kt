package rs.raf.sk.gdrive.resolver

import rs.raf.sk.model.RepositoryException
import rs.raf.sk.resolver.DependencyResolver
import rs.raf.sk.resolver.ResolverStorage
import java.io.IOException

/**
 * Drajver za repozitorijum na Google Drive-u.
 * Lokacija je ime korenskog foldera u My Drive-u, npr. "sk-repo".
 */
class GdriveResolver : DependencyResolver() {

    override val type: String = "gdrive"

    override fun createStorage(location: String): ResolverStorage {
        val drive = try {
            GoogleDriveAuth.connect()
        } catch (e: IOException) {
            throw RepositoryException("Cannot connect to Google Drive: ${e.message}", e)
        }
        return GdriveResolverStorage(drive, location)
    }
}
