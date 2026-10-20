package com.example.mylittlefinancontrol.Servicios

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

class SmsBancarioReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsBancarioReceiver"
        private const val CHANNEL_ID = "SMS_BANK_NOTIFICATIONS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: emptyArray()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (sms in messages) {
                        val sender = sms.originatingAddress ?: ""
                        val body = sms.messageBody ?: ""

                        Log.d(TAG, "SMS Recibido de: $sender -> $body")

                        if (esMensajeBancario(sender, body)) {
                            procesarSmsBancario(context, body)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en procesamiento asíncrono de SMS: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun esMensajeBancario(sender: String, body: String): Boolean {
        val senderUpper = sender.uppercase()
        val bodyUpper = body.uppercase()

        return senderUpper.contains("85540") ||
                bodyUpper.contains("BANCOLOMBIA") ||
                bodyUpper.contains("PAGASTE") ||
                bodyUpper.contains("TRANSFERISTE") ||
                bodyUpper.contains("RECIBISTE") ||
                bodyUpper.contains("COMPRA") ||
                bodyUpper.contains("TRANSFERENCIA")
    }

    private fun procesarSmsBancario(context: Context, body: String) {
        try {
            val datos = TransaccionBancariaParser.parsear(body)
            if (datos == null || datos.valor <= 0) {
                Log.w(TAG, "No se pudo extraer una transacción válida del SMS")
                return
            }

            val db = NotasDatabaseHelper.getInstance(context)

            // Validación inteligente de duplicados (tolerancia ±5 minutos)
            val duplicado = db.buscarMovimientoDuplicado(datos.valor, datos.tipo_reg, datos.fecha)
            if (duplicado != null) {
                Log.d(TAG, "SMS ignorado: Ya existe un movimiento idéntico registrado previamente (ID #${duplicado.id}, $${duplicado.valor}, ${duplicado.fecha})")
                return
            }

            val movimiento = Movimiento(
                id = 0,
                descripcion = datos.descripcion,
                valor = datos.valor,
                tipo_reg = datos.tipo_reg,
                fecha = datos.fecha,
                categoria = datos.categoria,
                fechaCreacion = Constantes.formatoFecha(System.currentTimeMillis())
            )

            db.insertNota(movimiento)
            Log.d(TAG, "Movimiento guardado automáticamente desde SMS: ${datos.descripcion} - $$${datos.valor}")

            mostrarNotificacion(context, datos.descripcion, datos.valor, datos.tipo_reg == "INGRESO")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando SMS bancario: ${e.message}", e)
        }
    }

    @Suppress("MissingPermission")
    @SuppressLint("MissingPermission")
    private fun mostrarNotificacion(context: Context, concepto: String, monto: Int, esIngreso: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones de SMS Bancarios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Muestra transacciones registradas automáticamente desde SMS bancarios"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val montoFormateado = format.format(monto)
        val simbolo = if (esIngreso) "+" else "-"
        val titulo = if (esIngreso) "💰 ¡Ingreso Detectado!" else "💳 ¡Transacción Detectada!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_movimientos)
            .setContentTitle(titulo)
            .setContentText("$concepto: $simbolo$montoFormateado")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Se registró automáticamente en tu control de finanzas:\n$concepto ($simbolo$montoFormateado)"))
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
                    Log.w(TAG, "Permiso POST_NOTIFICATIONS no concedido para mostrar notificación")
                    return
                }
            }
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "No se pudo mostrar notificación debido a permisos: ${e.message}")
        }
    }
}
