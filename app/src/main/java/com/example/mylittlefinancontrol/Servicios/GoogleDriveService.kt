package com.example.mylittlefinancontrol.Servicios

import android.content.Context
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.client.http.FileContent
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.Collections

class GoogleDriveService(context: Context, accountName: String) {

    private val driveService: Drive

    init {
        require(accountName.isNotBlank() && accountName != "null") {
            "Nombre de cuenta de Google no válido: '$accountName'"
        }
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).setSelectedAccountName(accountName)

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("My Little Finan Control").build()
    }

    /**
     * Sube un archivo a la carpeta de datos de la aplicación en Google Drive.
     */
    fun uploadFile(localFile: java.io.File, driveFileName: String): String? {
        val content = FileContent("application/x-sqlite3", localFile)
        
        // Buscar si ya existe para actualizarlo
        val existingFile = findFile(driveFileName)
        
        return if (existingFile != null) {
            // Para actualizar (PATCH), no enviamos 'parents' de nuevo ya que causaría conflicto
            val updateMetadata = File().apply {
                name = driveFileName
            }
            driveService.files().update(existingFile.id, updateMetadata, content).execute().id
        } else {
            // Para crear (POST)
            val createMetadata = File().apply {
                name = driveFileName
                parents = Collections.singletonList("appDataFolder")
            }
            driveService.files().create(createMetadata, content).execute().id
        }
    }

    /**
     * Descarga un archivo desde Google Drive usando un búfer de salida optimizado.
     */
    fun downloadFile(driveFileName: String, localFile: java.io.File): Boolean {
        val driveFile = findFile(driveFileName) ?: return false
        
        BufferedOutputStream(FileOutputStream(localFile), 8192).use { outputStream ->
            driveService.files().get(driveFile.id).executeMediaAndDownloadTo(outputStream)
            outputStream.flush()
        }
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
