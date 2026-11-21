package rs.raf.sk.gdrive.resolver

import com.google.api.services.drive.Drive
import rs.raf.sk.model.RepositoryException
import rs.raf.sk.resolver.ResolverStorage
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Skladište za repozitorijum na Google Drive-u, u folderu sa imenom [rootName] u korenu My Drive-a.
 *
 * Drive nema putanje nego ID-jeve, pa se putanja "rs.raf/utils/1.0.0" pronalazi
 * korak po korak: folder rs.raf u korenu, pa utils u njemu, pa 1.0.0.
 * Pronađeni ID-jevi se pamte u [ids], da se isti folder ne traži više puta.
 */
internal class GdriveResolverStorage(
    private val drive: Drive,
    private val rootName: String
) : ResolverStorage {

    private val ids = mutableMapOf<String, String>()     // putanja -> ID na Drive-u

    override fun exists(path: String): Boolean = call { find(path) != null }

    override fun read(path: String): ByteArray = call {
        val id = find(path) ?: throw RepositoryException("Cannot read file: $path")
        val output = ByteArrayOutputStream()
        drive.files().get(id).executeMediaAndDownloadTo(output)
        output.toByteArray()
    }

    override fun list(path: String): List<String> = call {
        val folderId = find(path) ?: return@call emptyList()
        val names = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val page = drive.files().list()
                .setQ("'$folderId' in parents and trashed = false")
                .setFields("nextPageToken, files(name)")
                .setPageToken(pageToken)
                .execute()
            page.files.mapTo(names) { it.name }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        names.distinct().sorted()
    }

    /** ID fajla ili foldera na putanji, ili null ako ne postoji. */
    private fun find(path: String): String? {
        var currentId = rootId() ?: return null
        var currentPath = ""
        for (part in parts(path)) {
            currentPath = join(currentPath, part)
            currentId = ids[currentPath]
                ?: findChild(currentId, part)?.also { ids[currentPath] = it } ?: return null
        }
        return currentId
    }

    /** ID korenskog foldera repozitorijuma, ili null ako ne postoji. */
    private fun rootId(): String? {
        ids[""]?.let { return it }
        return findChild("root", rootName)?.also { ids[""] = it }
    }

    /** ID stavke sa datim imenom direktno u folderu, ili null. Ako ih ima više, uzima najstariju. */
    private fun findChild(parentId: String, name: String): String? {
        val result = drive.files().list()
            .setQ("name = '${escape(name)}' and '$parentId' in parents and trashed = false")
            .setOrderBy("createdTime")
            .setPageSize(1)
            .setFields("files(id)")
            .execute()
        return result.files.firstOrNull()?.id
    }

    private fun parts(path: String): List<String> = if (path.isEmpty()) emptyList() else path.split("/")

    private fun join(parent: String, name: String): String = if (parent.isEmpty()) name else "$parent/$name"

    private fun escape(name: String): String = name.replace("\\", "\\\\").replace("'", "\\'")

    /** Greške Drive-a (mreža, dozvole) pretvara u RepositoryException, da CLI ispiše poruku. */
    private fun <T> call(block: () -> T): T =
        try {
            block()
        } catch (e: IOException) {
            throw RepositoryException("Google Drive error: ${e.message}", e)
        }
}
