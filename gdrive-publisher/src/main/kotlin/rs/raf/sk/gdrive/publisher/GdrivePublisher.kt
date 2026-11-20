package rs.raf.sk.gdrive.publisher

import rs.raf.sk.model.RepositoryException
import rs.raf.sk.publisher.Publisher
import rs.raf.sk.publisher.PublisherStorage
import java.io.IOException

/**
 * Drajver za repozitorijum na Google Drive-u.
 * Lokacija je ime korenskog foldera na Drive-u, npr. "sk-repo".
 */
class GdrivePublisher : Publisher() {

    override val type: String = "gdrive"

    override fun createStorage(location: String): PublisherStorage {
        val drive = try {
            GoogleDriveAuth.connect()
        } catch (e: IOException) {
            throw RepositoryException("Povezivanje sa Google Drive-om nije uspelo: ${e.message}", e)
        }
        return GdrivePublisherStorage(drive, location)
    }
}