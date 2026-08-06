package com.example.mylittlefinancontrol.Servicios

import android.content.Context
import android.util.Log
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupManager(private val context: Context) {

    private val dbName = "finanzas.db"

    /**
     * Prepara el archivo de base de datos para el respaldo.
     */
    fun getDatabaseFile(): File? {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return null
        
        // Aquí podríamos forzar un checkpoint de SQLite si fuera necesario,
        // pero para esta app sencilla con copiar el archivo suele bastar
        // si nos aseguramos de que no hay escrituras activas.
        return dbFile
    }

    /**
     * Sobreescribe la base de datos local con un archivo descargado.
     */
    fun restoreDatabase(downloadedFile: File): Boolean {
        try {
            val dbFile = context.getDatabasePath(dbName)
            
            // Cerrar la base de datos antes de sobreescribir
            NotasDatabaseHelper(context).close()
            
            // Copiar el archivo descargado a la ubicación de la base de datos
            val input = FileInputStream(downloadedFile)
            val output = FileOutputStream(dbFile)
            input.use { it.copyTo(output) }
            output.close()
            
            Log.d("BackupManager", "Base de datos restaurada con éxito")
            return true
        } catch (e: Exception) {
            Log.e("BackupManager", "Error al restaurar base de datos", e)
            return false
        }
    }
}
