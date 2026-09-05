package com.example.mylittlefinancontrol.Servicios

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("BackupWorker", "Iniciando tarea de respaldo automático...")
        val sp = applicationContext.getSharedPreferences("BackupPrefs", Context.MODE_PRIVATE)
        val accountName = sp.getString("GoogleAccount", null)

        if (accountName == null) {
            Log.w("BackupWorker", "Respaldo cancelado: No hay cuenta de Google vinculada")
            return@withContext Result.failure()
        }

        try {
            Log.d("BackupWorker", "Conectando a Google Drive para: $accountName")
            val driveService = GoogleDriveService(applicationContext, accountName)
            val backupManager = BackupManager(applicationContext)
            val dbFile = backupManager.getDatabaseFile()

            if (dbFile != null) {
                Log.d("BackupWorker", "Subiendo archivo de base de datos...")
                val fileId = driveService.uploadFile(dbFile, "finanzas_backup.db")
                if (fileId != null) {
                    Log.i("BackupWorker", "Respaldo completado con éxito. ID: $fileId")
                    sp.edit().putLong("LastBackupTime", System.currentTimeMillis()).apply()
                    return@withContext Result.success()
                } else {
                    Log.e("BackupWorker", "Error: La subida devolvió un ID nulo")
                }
            } else {
                Log.e("BackupWorker", "Error: No se pudo encontrar el archivo de la base de datos")
            }
            Log.w("BackupWorker", "Reintentando respaldo más tarde...")
            Result.retry()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Error crítico durante el respaldo: ${e.message}", e)
            Result.retry()
        }
    }
}
