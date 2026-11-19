package rs.raf.sk.local.publisher

import rs.raf.sk.publisher.Publisher
import rs.raf.sk.publisher.PublisherStorage
import java.io.File

/**
 * Drajver za repozitorijum na lokalnom disku.
 * Lokacija je putanja do korenskog foldera, npr. "./moj-repo".
 */
class LocalPublisher : Publisher() {

    override val type: String = "local"

    override fun createStorage(location: String): PublisherStorage =
        LocalPublisherStorage(File(location))

}