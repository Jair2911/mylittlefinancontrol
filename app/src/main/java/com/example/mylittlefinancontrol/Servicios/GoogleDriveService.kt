package com.example.mylittlefinancontrol.Servicios

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.client.http.FileContent
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections

class GoogleDriveService(context: Context, accountName: String) {

    private val driveService: Drive

    init {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).setSelectedAccountName(accountName)

        driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("My Little Finan Control").build()
    }

    /**
     * Sube un archivo a la carpeta de datos de la aplicación en Google Drive.
     */
    fun uploadFile(localFile: java.io.File, driveFileName: String): String? {
        val metadata = File().apply {
            name = driveFileName
            parents = Collections.singletonList("appDataFolder")
        }

        val content = FileContent("application/x-sqlite3", localFile)
        
        // Buscar si ya existe para actualizarlo
        val existingFile = findFile(driveFileName)
        
        return if (existingFile != null) {
            driveService.files().update(existingFile.id, metadata, content).execute().id
        } else {
            driveService.files().create(metadata, content).execute().id
        }
    }

    /**
     * Descarga un archivo desde Google Drive.
     */
    fun downloadFile(driveFileName: String, localFile: java.io.File): Boolean {
        val driveFile = findFile(driveFileName) ?: return false
        
        val outputStream: OutputStream = FileOutputStream(localFile)
        driveService.files().get(driveFile.id).executeMediaAndDownloadTo(outputStream)
        outputStream.close()
        return true
    }

    /**
     * Busca un archivo por nombre específicamente en la carpeta de la aplicación.
     */
    fun findFile(name: String): File? {
        val query = "name = '$name' and 'appDataFolder' in parents"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id, name, modifiedTime)")
            .execute()
        
        return result.files?.firstOrNull()
    }
}
