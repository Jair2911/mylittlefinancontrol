package com.example.mylittlefinancontrol

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object Constantes {
    fun obtenerTiempoD(): Long {
        return System.currentTimeMillis()
    }

    fun formatoFecha(tiempo: Long): String {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = tiempo
        return DateFormat.format("yyyy-MM-dd HH:mm:ss", calendar).toString()
    }

    /**
     * Parsea un string de monto ingresado por el usuario o precargado (ej. "60000.00", "$ 60.000,00", "60000").
     * Previene multiplicar por 100 al remover el separador decimal .00 o ,00.
     */
    fun limpiarYParsearMonto(rawInput: String): Double {
        var str = rawInput.replace("$", "").trim()
        if (str.isBlank()) return 0.0

        // Remover centavos no deseados .00 o ,00 al final
        if (str.endsWith(".00") || str.endsWith(",00")) {
            str = str.substring(0, str.length - 3)
        } else if (str.endsWith(".0") || str.endsWith(",0")) {
            str = str.substring(0, str.length - 2)
        }

        // Si tiene formato decimal puro de EE.UU. tipo "60000.5" o "60000.50"
        if (str.matches(Regex("^\\d+\\.\\d{1,2}$"))) {
            return redondearMonto(str.toDoubleOrNull() ?: 0.0)
        }

        // Remover puntos y comas de separadores de miles
        val soloNumeros = str.replace(".", "").replace(",", "").trim()
        val parsed = soloNumeros.toDoubleOrNull() ?: 0.0
        return redondearMonto(parsed)
    }

    /**
     * Redondea valores flotantes para evitar errores de imprecisión IEEE 754 (ej. 19999.99999 -> 20000.0)
     */
    fun redondearMonto(valor: Double): Double {
        return (valor * 100.0).roundToInt() / 100.0
    }
}
