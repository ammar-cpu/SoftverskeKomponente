package rs.raf.sk.gdrive.publisher

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.drive.Drive
import rs.raf.sk.model.ComponentNotFoundException
import rs.raf.sk.model.RepositoryException
import rs.raf.sk.publisher.PublisherStorage
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Skladiste za repozitorijum na Google Drive-u, u folderu sa imenom [rootName] u MyDrive.
 *
 * Drive nema putanje nego ID-jeve, pa se putanja "rs.raf/utils/1.0.0" pronalazi
 * korak po korak: folder rs.raf u korenu, pa utils u njemu, pa 1.0.0.
 * Pronadjeni ID-jevi se pamte u [ids], da se isti folder ne trazi vise puta.
 */
internal class GdrivePublisherStorage(
    private val drive: Drive,
    private val rootName: String
) : PublisherStorage {

    private val ids = mutableMapOf<String, String>()     // putanja -> ID na Drive-u

    override fun exists(path: String): Boolean = call { find(path) != null }

    override fun read(path: String): ByteArray = call {
        val id = find(path) ?: throw ComponentNotFoundException("Fajl ne postoji: $path")
        val output = ByteArrayOutputStream()
        drive.files().get(id).executeMediaAndDownloadTo(output)
        output.toByteArray()
    }

    override fun write(path: String, data: ByteArray): Unit = call {
        val parentPath = path.substringBeforeLast("/", "")
        val name = path.substringAfterLast("/")
        val parentId = find(parentPath) ?: throw RepositoryException("Folder ne postoji: $parentPath")
        val content = ByteArrayContent("application/octet-stream", data)

        val existingId = findChild(parentId, name)
        if (existingId == null) {
            val created = drive.files()
                .create(DriveFile().setName(name).setParents(listOf(parentId)), content)
                .setFields("id")
                .execute()
            ids[path] = created.id
        } else {
            drive.files().update(existingId, DriveFile(), content).execute()  // npr. izmena config.json
        }
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

    override fun createDir(path: String): Unit = call {
        var currentId = rootID(create = true)!!
        var currentPath = ""
        for (part in parts(path)) {
            currentPath = join(currentPath, part)
            currentId = ids[currentPath]
                ?: (findChild(currentId, part) ?: createFolder(currentId, part)).also { ids[currentPath] = it }
        }
    }

    // POMOCNE

    /** ID fajla ili foldera na putanji, ili null ako ne postoji. */
    private fun find(path: String): String? {
        var currentId = rootID(create = false) ?: return null
        var currentPath = ""
        for (part in parts(path)) {
            currentPath = join(currentPath, part)
            currentId = ids[currentPath]
                ?: findChild(currentId, part)?.also { ids[currentPath] = it } ?: return null
        }
        return currentId
    }

    /** ID korenskog foldera repozitorijuma; pravi ga ako ne postoji i [create] je true. */
    private fun rootID(create: Boolean): String? {
        ids[""]?.let { return it }
        val id = findChild("root", rootName)
            ?: if (create) createFolder("root", rootName) else return null
        ids[""] = id
        return id
    }

    /** ID stavke sa datim imenom direktno u folderu, ili null. */
    private fun findChild(parentId: String, name: String): String? {
        val result = drive.files().list()
            .setQ("name = '${escape(name)}' and '$parentId' in parents and trashed = false")
            .setOrderBy("createdTime")
            .setPageSize(1)
            .setFields("files(id)")
            .execute()
        return result.files.firstOrNull()?.id

    }

    private fun createFolder(parentId: String, name: String): String =
        drive.files()
            .create(DriveFile().setName(name).setMimeType(FOLDER).setParents(listOf(parentId)))
            .setFields("id")
            .execute()
            .id

    private fun parts(path: String): List<String> = if (path.isEmpty()) emptyList() else path.split("/")

    private fun join(parent: String, name: String): String = if (parent.isEmpty()) name else "$parent/$name"

    private fun escape(name: String): String = name.replace("\\", "\\\\").replace("'", "\\'")

    /** Greske Drive-a (mreza, dozvole) pretvara u RepositoryException, da CLI ispise lepu poruku. */
    private fun <T> call(block: () -> T): T =
        try {
            block()
        } catch (e: IOException) {
            throw RepositoryException("Google Drive greška: ${e.message}", e)
        }

    private companion object {
        const val FOLDER = "application/vnd.google-apps.folder"
    }

}