package com.example.mylittlefinancontrol.Servicios

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class GastoFijoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "GastoFijoWorker"
        private const val CHANNEL_ID = "GASTOS_FIJOS_REMINDERS"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando verificación de recordatorios de Gastos Fijos...")
        try {
            verificarYNotificarGastosFijos(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error en verificación de Gastos Fijos: ${e.message}", e)
            Result.failure()
        }
    }

    private fun verificarYNotificarGastosFijos(context: Context) {
        val db = NotasDatabaseHelper(context)
        val gastosFijos = db.getAllGastosFijos()

        val cal = Calendar.getInstance()
        val hoyDia = cal.get(Calendar.DAY_OF_MONTH)
        val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        for (gasto in gastosFijos) {
            if (!gasto.recordatorio) continue

            // Verificar si ya fue pagado este mes
            val yaPagado = if (gasto.periodo.contains("Quincenal", true)) {
                gasto.ultimoPagoQ1 == mesAnioActual && gasto.ultimoPagoQ2 == mesAnioActual
            } else {
                gasto.ultimoPagoQ1 == mesAnioActual
            }

            if (yaPagado) continue

            // Extraer el día numérico de pago
            val diaNum = Regex("\\d+").find(gasto.diaPago)?.value?.toIntOrNull() ?: 15
            val diasRestantes = if (diaNum >= hoyDia) {
                diaNum - hoyDia
            } else {
                (30 - hoyDia) + diaNum
            }

            // Notificar si está dentro del rango de 3 días antes o el mismo día del vencimiento
            if (diasRestantes in 0..3) {
                val textoDias = when (diasRestantes) {
                    0 -> "¡Vence HOY!"
                    1 -> "vence MAÑANA"
                    else -> "vence en $diasRestantes días"
                }

                val titulo = "⏰ Recordatorio: ${gasto.concepto}"
                val mensaje = "Tu compromiso de ${format.format(gasto.monto)} $textoDias (Día $diaNum). ¡No olvides registrar tu pago!"

                mostrarNotificacion(context, gasto.id.hashCode(), titulo, mensaje)
            }
        }
    }

    @Suppress("MissingPermission")
    @SuppressLint("MissingPermission")
    private fun mostrarNotificacion(context: Context, notifId: Int, titulo: String, mensaje: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Gastos Fijos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones automáticas de vencimiento de compromisos fijos"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gastos_fijos)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "Permiso POST_NOTIFICATIONS no concedido para recordatorio")
                    return
                }
            }
            notificationManager.notify(notifId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando notificación de gasto fijo: ${e.message}")
        }
    }
}
