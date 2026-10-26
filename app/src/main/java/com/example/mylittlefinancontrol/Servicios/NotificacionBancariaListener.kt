package com.example.mylittlefinancontrol.Servicios

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.Movimiento
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificacionBancariaListener : NotificationListenerService() {

    private val TAG = "NotificacionBancariaListener"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extraer título y texto de la notificación
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullText = "$title $text $bigText"

        Log.d(TAG, "Notificación detectada [$packageName]: $fullText")

        // Filtrar paquetes si es necesario (p.ej. solo procesar apps bancarias o de SMS conocidos)
        // Por ahora, le enviamos todo al parser y él decide si es una transacción válida
        procesarNotificacion(fullText)
    }

    private fun procesarNotificacion(texto: String) {
        if (texto.isBlank()) return

        val datos = TransaccionBancariaParser.parsear(texto)
        if (datos == null || datos.valor <= 0) {
            // No es una transacción bancaria válida
            return
        }

        val db = NotasDatabaseHelper.getInstance(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            // Buscar duplicados usando la ventana de ±5 minutos
            val duplicado = db.buscarMovimientoDuplicado(datos.valor, datos.fecha)
            if (duplicado != null) {
                Log.d(TAG, "Notificación ignorada: Ya existe un movimiento idéntico registrado (ID #${duplicado.id}, $${duplicado.valor})")
                return@launch
            }

            // Guardar automáticamente
            try {
                val mov = Movimiento(
                    id = 0,
                    descripcion = datos.descripcion.uppercase(),
                    valor = datos.valor,
                    tipo_reg = datos.tipo_reg,
                    fecha = datos.fecha,
                    categoria = datos.categoria,
                    fechaCreacion = com.example.mylittlefinancontrol.Constantes.formatoFecha(System.currentTimeMillis())
                )
                db.insertNota(mov)
                Log.d(TAG, "Movimiento guardado automáticamente desde Notificación: ${datos.descripcion} - $${datos.valor}")
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando movimiento desde Notificación: ${e.message}", e)
            }
        }
    }
}