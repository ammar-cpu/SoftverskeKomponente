package rs.raf.sk.gdrive.resolver

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import rs.raf.sk.model.RepositoryException
import java.io.File

/**
 * OAuth prijava na Google Drive, isto kao u gdrive-publisher, ali samo za čitanje.
 * Konfiguracija se čita iz GOOGLE_DRIVE_CREDENTIALS ili config/credentials.json, a tokeni se čuvaju
 * u tokens-resolver/, odvojeno od publisher-ovih, jer imaju drugi scope.
 * Putanje su relativne u odnosu na folder iz kog se program pokreće.
 */
internal object GoogleDriveAuth {

    fun connect(): Drive {
        val json = GsonFactory.getDefaultInstance()
        val client = File(System.getenv("GOOGLE_DRIVE_CREDENTIALS") ?: "config/credentials.json")
            .reader().use { GoogleClientSecrets.load(json, it).installed }
            ?: throw RepositoryException("credentials.json must be an OAuth client of type Desktop app")
        val transport = NetHttpTransport()

        val flow = AuthorizationCodeFlow.Builder(
            BearerToken.authorizationHeaderAccessMethod(), transport, json,
            GenericUrl(GoogleOAuthConstants.TOKEN_SERVER_URL),
            ClientParametersAuthentication(client.clientId, client.clientSecret),
            client.clientId,
            GoogleOAuthConstants.AUTHORIZATION_SERVER_URL + "?access_type=offline&prompt=consent"
        ).setScopes(listOf(DriveScopes.DRIVE_READONLY))
            .setDataStoreFactory(FileDataStoreFactory(File("tokens-resolver")))
            .enablePKCE()
            .build()

        val receiver = LocalServerReceiver.Builder().setHost("127.0.0.1").setPort(-1).build()
        val credential = AuthorizationCodeInstalledApp(flow, receiver).authorize(client.clientId)
        return Drive.Builder(transport, json, credential).setApplicationName("SK Resolver").build()
    }
}
