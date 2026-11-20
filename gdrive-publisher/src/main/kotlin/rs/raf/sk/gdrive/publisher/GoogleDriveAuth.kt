package rs.raf.sk.gdrive.publisher

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
import java.io.File

/**
 * OAuth prijava na Google Drive, preuzeta iz primera sa vežbi.
 * Konfiguracija se čita iz config/credentials.json, a tokeni se čuvaju u tokens/,
 * relativno u odnosu na folder iz kog se program pokreće.
 */
internal object GoogleDriveAuth {

    fun connect(): Drive {
        val json = GsonFactory.getDefaultInstance()
        val client = File(System.getenv("GOOGLE_DRIVE_CREDENTIALS") ?: "config/credentials.json")
            .reader().use { GoogleClientSecrets.load(json, it).installed }
            ?: error("Potreban je OAuth JSON za Desktop app.")
        val transport = NetHttpTransport()

        val flow = AuthorizationCodeFlow.Builder(
            BearerToken.authorizationHeaderAccessMethod(), transport, json,
            GenericUrl(GoogleOAuthConstants.TOKEN_SERVER_URL),
            ClientParametersAuthentication(client.clientId, client.clientSecret),
            client.clientId,
            GoogleOAuthConstants.AUTHORIZATION_SERVER_URL + "?access_type=offline&prompt=consent"
        ).setScopes(listOf(DriveScopes.DRIVE_FILE))
            .setDataStoreFactory(FileDataStoreFactory(File(System.getenv("GOOGLE_DRIVE_TOKENS") ?: "tokens")))
            .enablePKCE()
            .build()

        val receiver = LocalServerReceiver.Builder().setHost("127.0.0.1").setPort(-1).build()
        val credential = AuthorizationCodeInstalledApp(flow, receiver).authorize(client.clientId)
        return Drive.Builder(transport, json, credential).setApplicationName("SK Publisher").build()
    }
}