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
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.originatingAddress ?: ""
                val body = sms.messageBody ?: ""

                Log.d(TAG, "SMS Recibido de: $sender -> $body")

                // Verificar si el mensaje proviene de Bancolombia u otro banco
                if (esMensajeBancario(sender, body)) {
                    procesarSmsBancario(context, body)
                }
            }
        }
    }

    private fun esMensajeBancario(sender: String, body: String): Boolean {
        val senderUpper = sender.uppercase()
        val bodyUpper = body.uppercase()

        return senderUpper.contains("85540") ||
                bodyUpper.contains("BANCOLOMBIA") ||
                bodyUpper.contains("PAGASTE $") ||
                bodyUpper.contains("RECIBISTE UN PAGO") ||
                bodyUpper.contains("COMPRA POR $") ||
                bodyUpper.contains("TRANSFERENCIA")
    }

    private fun procesarSmsBancario(context: Context, body: String) {
        try {
            val bodyUpper = body.uppercase()

            // 1. Determinar Tipo (INGRESO o GASTO)
            val esIngreso = bodyUpper.contains("RECIBISTE UN PAGO") ||
                    bodyUpper.contains("ABONO") ||
                    bodyUpper.contains("CONSIGNACION") ||
                    bodyUpper.contains("TRANSFERENCIA RECIBIDA") ||
                    bodyUpper.contains("NÓMINA") ||
                    bodyUpper.contains("NOMINA")

            val tipoReg = if (esIngreso) "INGRESO" else "GASTO"

            // 2. Extraer Valor/Monto ($X,XXX.XX)
            val regexMonto = Pattern.compile("\\$\\s?([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)")
            val matcherMonto = regexMonto.matcher(body)
            var valorInt = 0

            if (matcherMonto.find()) {
                val montoRaw = matcherMonto.group(1) ?: "0"
                // Quitar comas y decimales
                val montoLimpio = montoRaw.replace(",", "").split(".")[0]
                valorInt = montoLimpio.toIntOrNull() ?: 0
            }

            if (valorInt <= 0) {
                Log.w(TAG, "No se pudo extraer un monto válido del SMS")
                return
            }

            // 3. Extraer Concepto o Detalle
            var concepto = if (esIngreso) "INGRESO BANCARIO" else "GASTO BANCOLOMBIA"
            var categoria = "Varios"

            if (bodyUpper.contains("PAGO DE NOMINA") || bodyUpper.contains("NOMINA")) {
                concepto = "PAGO NÓMINA"
                if (body.contains("de ", ignoreCase = true)) {
                    val partes = body.split("de ", ignoreCase = true)
                    if (partes.size > 1) {
                        val entidad = partes[1].split(" por")[0].trim()
                        concepto = "NÓMINA $entidad".uppercase()
                    }
                }
                categoria = "Ingresos"
            } else if (bodyUpper.contains("CODIGO QR") || bodyUpper.contains("CÓDIGO QR")) {
                concepto = "PAGO CÓDIGO QR BANCOLOMBIA"
                categoria = "Comida"
            } else if (bodyUpper.contains("COMPRA EN")) {
                val partes = body.split("COMPRA EN", ignoreCase = true)
                if (partes.size > 1) {
                    val lugar = partes[1].split(" POR")[0].trim()
                    concepto = "COMPRA EN $lugar".uppercase()
                }
            }

            // 4. Extraer Fecha del SMS si está presente (ej: 29/09/2026 a las 10:12)
            var fechaFinal = Constantes.formatoFecha(System.currentTimeMillis())
            val regexFecha = Pattern.compile("([0-9]{2}/[0-9]{2}/[0-9]{4}).+?([0-9]{2}:[0-9]{2})")
            val matcherFecha = regexFecha.matcher(body)

            if (matcherFecha.find()) {
                val fStr = matcherFecha.group(1)
                val hStr = matcherFecha.group(2)
                if (fStr != null && hStr != null) {
                    // Convertir DD/MM/YYYY a YYYY-MM-DD para consistencia
                    val partesF = fStr.split("/")
                    if (partesF.size == 3) {
                        fechaFinal = "${partesF[2]}-${partesF[1]}-${partesF[0]} $hStr:00"
                    }
                }
            }

            // 5. Guardar en Base de Datos Local
            val db = NotasDatabaseHelper(context)
            val movimiento = Movimiento(
                id = 0,
                descripcion = concepto,
                valor = valorInt,
                tipo_reg = tipoReg,
                fecha = fechaFinal,
                categoria = categoria,
                fechaCreacion = Constantes.formatoFecha(System.currentTimeMillis())
            )

            db.insertNota(movimiento)
            Log.d(TAG, "Movimiento guardado automáticamente desde SMS: $concepto - $$valorInt")

            // 6. Lanzar Notificación
            mostrarNotificacion(context, concepto, valorInt, esIngreso)

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
