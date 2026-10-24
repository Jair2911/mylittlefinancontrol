package com.example.mylittlefinancontrol.Servicios

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class DatosTransaccion(
    val valor: Int,
    val fecha: String, // Formato "yyyy-MM-dd HH:mm"
    val tipo_reg: String, // "GASTO" o "INGRESO"
    val descripcion: String,
    val categoria: String
)

object TransaccionBancariaParser {

    private const val TAG = "TransaccionParser"

    /**
     * Parsea texto proveniente de un SMS bancario o del escaneo OCR de ML Kit de un comprobante.
     */
    fun parsear(textoCompleto: String): DatosTransaccion? {
        if (textoCompleto.isBlank()) return null

        val valor = extraerValor(textoCompleto)
        if (valor <= 0) {
            Log.w(TAG, "No se pudo extraer un valor numérico válido del texto")
            return null
        }

        val tipoReg = extraerTipo(textoCompleto)
        val fecha = extraerFecha(textoCompleto)
        val (descripcion, categoria) = extraerDescripcionYCategoria(textoCompleto, tipoReg)

        return DatosTransaccion(
            valor = valor,
            fecha = fecha,
            tipo_reg = tipoReg,
            descripcion = descripcion,
            categoria = categoria
        )
    }

    /**
     * Extrae el valor numérico:
     * 1. Detecta cantidades con símbolo $ o palabras clave (pagaste, transferiste, valor, etc.).
     * 2. Limpia los decimales finales (.00 o ,00 con Regex [.,]\d{2}\b).
     * 3. Remueve puntos/comas de miles para que "$5,800.00" sea 5800.
     */
    fun extraerValor(texto: String): Int {
        val patrones = listOf(
            // Patrón con palabra clave y monto
            Pattern.compile("(?:pagaste|transferiste|compraste|retiraste|enviaste|recibiste|abono|por|valor|total|monto)\\s*[:\\$]?\\s*\\$?([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]+(?:[.,][0-9]{2})?)", Pattern.CASE_INSENSITIVE),
            // Patrón con signo pesos
            Pattern.compile("\\$\\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]+(?:[.,][0-9]{2})?)"),
            // Patrón de números formateados con separador de miles
            Pattern.compile("\\b([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{2})?)\\b")
        )

        for (pattern in patrones) {
            val matcher = pattern.matcher(texto)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: continue
                val limpio = limpiarMontoTexto(candidate)
                if (limpio > 0) return limpio
            }
        }

        return 0
    }

    private fun limpiarMontoTexto(montoRaw: String): Int {
        var str = montoRaw.trim()
        // 1. Limpia primero los decimales finales (.00 o ,00)
        str = str.replace(Regex("[.,]\\d{2}\\b"), "")
        // 2. Quitar puntos y comas de miles y cualquier símbolo
        val soloDigitos = str.replace(Regex("[^0-9]"), "")
        return soloDigitos.toIntOrNull() ?: 0
    }

    /**
     * Determina si la transacción es GASTO o INGRESO según palabras clave.
     */
    fun extraerTipo(texto: String): String {
        val lower = texto.lowercase(Locale.ROOT)
        val esIngreso = lower.contains("recibiste") ||
                lower.contains("abono") ||
                lower.contains("consignacion") ||
                lower.contains("consignación") ||
                lower.contains("transferencia recibida") ||
                lower.contains("pago recibido") ||
                lower.contains("te transfirieron") ||
                lower.contains("te enviaron") ||
                lower.contains("nomina") ||
                lower.contains("nómina") ||
                lower.contains("ingreso")

        return if (esIngreso) "INGRESO" else "GASTO"
    }

    /**
     * Extrae fecha y hora del texto y la normaliza a "yyyy-MM-dd HH:mm".
     * Soporta formatos:
     * - "19/10/2026 a las 17:11" o "19/10/2026 17:11"
     * - "19 de octubre de 2026 17:11" o "19 oct 2026 - 17:11"
     * - "2026-10-19 17:11"
     */
    fun extraerFecha(texto: String): String {
        // Formato 1: dd/MM/yyyy [a las |,]? HH:mm
        val p1 = Pattern.compile("([0-3]?[0-9])[/-]([0-1]?[0-9])[/-](20\\d{2}|\\d{2})(?:[\\s,a-zA-Z-]{1,15}?([0-2]?[0-9]):([0-5][0-9])(?::[0-5][0-9])?(?:\\s*([aApP][.\\s]*[mM]\\.?))?)?")
        val m1 = p1.matcher(texto)
        if (m1.find()) {
            val dia = m1.group(1)?.toIntOrNull() ?: 1
            val mes = m1.group(2)?.toIntOrNull() ?: 1
            var anio = m1.group(3)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            if (anio < 100) anio += 2000

            var hora = m1.group(4)?.toIntOrNull()
            val minuto = m1.group(5)?.toIntOrNull() ?: 0
            val amPmRaw = m1.group(6)

            if (hora == null) {
                val cal = Calendar.getInstance()
                hora = cal.get(Calendar.HOUR_OF_DAY)
            } else {
                if (amPmRaw != null) {
                    val amPm = amPmRaw.replace(Regex("[.\\s]"), "").lowercase()
                    if (amPm == "pm" && hora < 12) hora += 12
                    if (amPm == "am" && hora == 12) hora = 0
                }
            }

            return String.format(Locale.US, "%04d-%02d-%02d %02d:%02d", anio, mes, dia, hora, minuto)
        }

        // Formato 2: 19 de octubre de 2026 17:11 / 19 oct 2026 - 17:11
        val p2 = Pattern.compile("([0-3]?[0-9])\\s+(?:de\\s+)?([a-zA-ZáéíóúÁÉÍÓÚ]{3,10})\\s+(?:de\\s+)?(20\\d{2})(?:[\\s,a-zA-Z-]{1,15}?([0-2]?[0-9]):([0-5][0-9])(?:\\s*([aApP][.\\s]*[mM]\\.?))?)?", Pattern.CASE_INSENSITIVE)
        val m2 = p2.matcher(texto)
        if (m2.find()) {
            val dia = m2.group(1)?.toIntOrNull() ?: 1
            val mesStr = m2.group(2)?.lowercase(Locale.ROOT) ?: ""
            val mes = obtenerMesDesdeTexto(mesStr)
            val anio = m2.group(3)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

            var hora = m2.group(4)?.toIntOrNull()
            val minuto = m2.group(5)?.toIntOrNull() ?: 0
            val amPmRaw = m2.group(6)

            if (hora == null) {
                val cal = Calendar.getInstance()
                hora = cal.get(Calendar.HOUR_OF_DAY)
            } else {
                if (amPmRaw != null) {
                    val amPm = amPmRaw.replace(Regex("[.\\s]"), "").lowercase()
                    if (amPm == "pm" && hora < 12) hora += 12
                    if (amPm == "am" && hora == 12) hora = 0
                }
            }

            return String.format(Locale.US, "%04d-%02d-%02d %02d:%02d", anio, mes, dia, hora, minuto)
        }

        // Formato 3: yyyy-MM-dd HH:mm
        val p3 = Pattern.compile("(20\\d{2})[/-]([0-1]?[0-9])[/-]([0-3]?[0-9])(?:[\\s,a-zA-Z-]{1,15}?([0-2]?[0-9]):([0-5][0-9]))?")
        val m3 = p3.matcher(texto)
        if (m3.find()) {
            val anio = m3.group(1)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            val mes = m3.group(2)?.toIntOrNull() ?: 1
            val dia = m3.group(3)?.toIntOrNull() ?: 1
            val hora = m3.group(4)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val minuto = m3.group(5)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MINUTE)
            return String.format(Locale.US, "%04d-%02d-%02d %02d:%02d", anio, mes, dia, hora, minuto)
        }

        // Fallback: Fecha actual del sistema
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }

    private fun obtenerMesDesdeTexto(nombre: String): Int {
        val n = nombre.lowercase(Locale.ROOT)
        return when {
            n.startsWith("ene") -> 1
            n.startsWith("feb") -> 2
            n.startsWith("mar") -> 3
            n.startsWith("abr") -> 4
            n.startsWith("may") -> 5
            n.startsWith("jun") -> 6
            n.startsWith("jul") -> 7
            n.startsWith("ago") -> 8
            n.startsWith("sep") || n.startsWith("set") -> 9
            n.startsWith("oct") -> 10
            n.startsWith("nov") -> 11
            n.startsWith("dic") -> 12
            else -> 1
        }
    }

    /**
     * Extrae descripción detallada y categoría contextual.
     * Ejemplo SMS Bancolombia:
     * - "pagaste $5,800.00 por codigo qr desde tu cuenta *0261 a la clave 0091988741" -> "Pago QR - Clave 0091988741"
     * - "transferiste $11,500.00 desde tu cuenta *0261 a la cuenta *3176409745" -> "Transferencia a *3176409745"
     */
    fun extraerDescripcionYCategoria(texto: String, tipoReg: String): Pair<String, String> {
        val upper = texto.uppercase(Locale.ROOT)

        // 1. Caso Pago QR con clave
        val mQrClave = Pattern.compile("(?:A LA CLAVE|CLAVE)\\s*([0-9a-zA-Z]+)", Pattern.CASE_INSENSITIVE).matcher(texto)
        if (mQrClave.find()) {
            val clave = mQrClave.group(1)?.trim() ?: ""
            return Pair("Pago QR - Clave $clave", "Comida")
        }

        if (upper.contains("CODIGO QR") || upper.contains("CÓDIGO QR")) {
            return Pair("Pago Código QR", "Comida")
        }

        // 2. Caso Transferencia con cuenta destino
        val mCuentaDestino = Pattern.compile("(?:A LA CUENTA|CUENTA DESTINO)\\s*([*0-9a-zA-Z]+)", Pattern.CASE_INSENSITIVE).matcher(texto)
        if (mCuentaDestino.find()) {
            val cuenta = mCuentaDestino.group(1)?.trim() ?: ""
            return Pair("Transferencia a $cuenta", "Varios")
        }

        // 3. Caso Comprobante: Destino o Para
        val mDestinoComprobante = Pattern.compile("(?:DESTINO|PARA|A FAVOR DE|BENEFICIARIO):?\\s*([^\\n\\r]+)", Pattern.CASE_INSENSITIVE).matcher(texto)
        if (mDestinoComprobante.find()) {
            val dest = mDestinoComprobante.group(1)?.trim() ?: ""
            if (dest.isNotBlank() && dest.length < 50) {
                val cat = clasificarCategoriaPorNombre(dest)
                return Pair("Transferencia a $dest", cat)
            }
        }

        // 4. Caso Compra en Comercio
        val mCompraEn = Pattern.compile("COMPRA EN\\s+([^\\n\\r,.]+?)(?:\\s+POR|\\s+\\$|\\s+EL|\\.|,|$)", Pattern.CASE_INSENSITIVE).matcher(texto)
        if (mCompraEn.find()) {
            val comercio = mCompraEn.group(1)?.trim() ?: ""
            if (comercio.isNotBlank()) {
                val cat = clasificarCategoriaPorNombre(comercio)
                return Pair("Compra en $comercio", cat)
            }
        }

        val mComercio = Pattern.compile("(?:COMERCIO|ESTABLECIMIENTO):?\\s*([^\\n\\r]+)", Pattern.CASE_INSENSITIVE).matcher(texto)
        if (mComercio.find()) {
            val comercio = mComercio.group(1)?.trim() ?: ""
            if (comercio.isNotBlank() && comercio.length < 50) {
                val cat = clasificarCategoriaPorNombre(comercio)
                return Pair("Compra en $comercio", cat)
            }
        }

        // 5. Caso Nómina
        if (upper.contains("PAGO DE NOMINA") || upper.contains("PAGO DE NÓMINA") || upper.contains("NOMINA")) {
            val mNomina = Pattern.compile("(?:DE NOMINA DE|DE NÓMINA DE|DE)\\s+([^\\n\\r,.]+?)(?:\\s+POR|\\s+\\$|\\s+EL|\\.|,|$)", Pattern.CASE_INSENSITIVE).matcher(texto)
            val empresa = if (mNomina.find()) mNomina.group(1)?.trim() ?: "" else ""
            val desc = if (empresa.isNotBlank() && empresa.length < 40) "Nómina $empresa" else "Pago Nómina"
            return Pair(desc, "Ingresos")
        }

        // 6. Clasificación general por palabras clave
        val catAuto = clasificarCategoriaPorTexto(upper)
        val descDefault = when {
            tipoReg == "INGRESO" -> "Ingreso Bancario"
            upper.contains("TRANSFERENCIA") || upper.contains("TRANSFERISTE") -> "Transferencia Bancaria"
            upper.contains("COMPRA") || upper.contains("COMPRASTE") -> "Compra Bancaria"
            upper.contains("RETIRO") || upper.contains("RETIRASTE") -> "Retiro Cajero"
            else -> "Gasto Bancolombia"
        }

        return Pair(descDefault, catAuto)
    }

    private fun clasificarCategoriaPorNombre(nombre: String): String {
        val u = nombre.uppercase(Locale.ROOT)
        return when {
            u.contains("EXITO") || u.contains("ÉXITO") || u.contains("JUMBO") || u.contains("D1") ||
                    u.contains("ARA") || u.contains("OLIMPICA") || u.contains("OLÍMPICA") ||
                    u.contains("CARULLA") || u.contains("ALKOSTO") || u.contains("SUPERMERCADO") -> "Supermercado"

            u.contains("RAPPI") || u.contains("DIDIFOOD") || u.contains("UBEREATS") ||
                    u.contains("RESTAURANTE") || u.contains("MCDONALD") || u.contains("SUBWAY") ||
                    u.contains("FRISBY") || u.contains("CORRAL") || u.contains("KFC") ||
                    u.contains("PANADERIA") || u.contains("CAFETERIA") || u.contains("COMIDA") -> "Comida"

            u.contains("CLARO") || u.contains("TIGO") || u.contains("MOVISTAR") ||
                    u.contains("EPM") || u.contains("ENEL") || u.contains("CODENSA") ||
                    u.contains("GAS") || u.contains("ACUEDUCTO") || u.contains("ENERGIA") ||
                    u.contains("INTERNET") -> "Servicios"

            u.contains("UBER") || u.contains("DIDI") || u.contains("CABIFY") ||
                    u.contains("TERPEL") || u.contains("PRIMAX") || u.contains("BIOMAX") ||
                    u.contains("GASOLINA") || u.contains("PEAJE") -> "Transporte"

            u.contains("FARMACIA") || u.contains("DROGUERIA") || u.contains("DROGUERÍA") ||
                    u.contains("CRUZ VERDE") || u.contains("LA REBAJA") || u.contains("EPS") ||
                    u.contains("HOSPITAL") || u.contains("CLINICA") || u.contains("MEDICO") -> "Salud"

            u.contains("NETFLIX") || u.contains("SPOTIFY") || u.contains("DISNEY") ||
                    u.contains("HBO") || u.contains("PRIME VIDEO") || u.contains("YOUTUBE") ||
                    u.contains("APPLE") -> "Streaming"

            u.contains("UNIVERSIDAD") || u.contains("COLEGIO") || u.contains("CURSO") ||
                    u.contains("PLATZI") || u.contains("UDEMY") -> "Educación"

            else -> "Varios"
        }
    }

    private fun clasificarCategoriaPorTexto(upper: String): String {
        return when {
            upper.contains("SUPERMERCADO") || upper.contains("EXITO") || upper.contains("CARULLA") || upper.contains("D1") -> "Supermercado"
            upper.contains("CODIGO QR") || upper.contains("CÓDIGO QR") || upper.contains("RESTAURANTE") || upper.contains("RAPPI") -> "Comida"
            upper.contains("SERVICIOS") || upper.contains("EPM") || upper.contains("ENEL") || upper.contains("CLARO") -> "Servicios"
            upper.contains("GASOLINA") || upper.contains("UBER") || upper.contains("TRANSPORTE") -> "Transporte"
            upper.contains("FARMACIA") || upper.contains("DROGUERIA") || upper.contains("SALUD") -> "Salud"
            upper.contains("NOMINA") || upper.contains("NÓMINA") -> "Ingresos"
            else -> "Varios"
        }
    }
}
