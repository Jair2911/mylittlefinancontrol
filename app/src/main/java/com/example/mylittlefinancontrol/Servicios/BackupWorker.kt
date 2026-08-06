package com.example.mylittlefinancontrol.Servicios

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sp = applicationContext.getSharedPreferences("BackupPrefs", Context.MODE_PRIVATE)
        val accountName = sp.getString("GoogleAccount", null) ?: return@withContext Result.failure()

        try {
            val driveService = GoogleDriveService(applicationContext, accountName)
            val backupManager = BackupManager(applicationContext)
            val dbFile = backupManager.getDatabaseFile()

            if (dbFile != null) {
                val fileId = driveService.uploadFile(dbFile, "finanzas_backup.db")
                if (fileId != null) {
                    sp.edit().putLong("LastBackupTime", System.currentTimeMillis()).apply()
                    return@withContext Result.success()
                }
            }
            Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
