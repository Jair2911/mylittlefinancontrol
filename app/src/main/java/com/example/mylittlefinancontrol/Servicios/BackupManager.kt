package com.example.mylittlefinancontrol.Servicios

import android.content.Context
import android.util.Log
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupManager(private val context: Context) {

    private val dbName = "finanzas.db"

    /**
     * Prepara el archivo de base de datos para el respaldo, ejecutando un checkpoint de WAL previo.
     */
    fun getDatabaseFile(): File? {
        try {
            // Sincronizar el WAL a la base de datos principal antes de copiar
            NotasDatabaseHelper.getInstance(context).performWalCheckpoint()
        } catch (e: Exception) {
            Log.e("BackupManager", "Error al ejecutar WAL Checkpoint antes de obtener el archivo", e)
        }

        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return null
        return dbFile
    }

    /**
     * Sobreescribe la base de datos local con un archivo descargado usando buffers de I/O eficientes.
     */
    fun restoreDatabase(downloadedFile: File): Boolean {
        try {
            val dbFile = context.getDatabasePath(dbName)
            
            // Eliminar archivos journal/wal/shm si existen antes de sobreescribir
            val journalFile = File(dbFile.path + "-journal")
            if (journalFile.exists()) journalFile.delete()

            val walFile = File(dbFile.path + "-wal")
            if (walFile.exists()) walFile.delete()

            val shmFile = File(dbFile.path + "-shm")
            if (shmFile.exists()) shmFile.delete()

            // Copiar el archivo descargado usando buffers de lectura y escritura para optimizar memoria en eMMC 5.1 / RAM
            BufferedInputStream(FileInputStream(downloadedFile), 8192).use { input ->
                BufferedOutputStream(FileOutputStream(dbFile), 8192).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            
            Log.d("BackupManager", "Base de datos restaurada con éxito")
            NotasDatabaseHelper.getInstance(context).invalidateCache()
            return true
        } catch (e: Exception) {
            Log.e("BackupManager", "Error al restaurar base de datos", e)
            return false
        }
    }
}
