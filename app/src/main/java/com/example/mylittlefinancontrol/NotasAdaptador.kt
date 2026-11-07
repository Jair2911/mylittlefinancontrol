package com.example.mylittlefinancontrol

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

sealed class MovimientoListItem {
    data class Header(val fechaTitulo: String, val conteoTexto: String) : MovimientoListItem()
    data class Item(val movimiento: Movimiento) : MovimientoListItem()
}

object NotasAdaptador {
    private val formatHora = SimpleDateFormat("HH:mm 'hrs'", Locale("es", "ES"))

    fun normalizarFechaClave(fechaRaw: String): String {
        if (fechaRaw.isBlank()) return "Sin Fecha"
        val timestamp = fechaRaw.trim().toLongOrNull()
        if (timestamp != null && timestamp > 100000000000L) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date(timestamp))
        }
        val clean = if (fechaRaw.length >= 10) fechaRaw.substring(0, 10) else fechaRaw.split(" ")[0]
        val delimiters = charArrayOf('-', '/', '.')
        for (d in delimiters) {
            if (clean.contains(d)) {
                val parts = clean.split(d)
                if (parts.size >= 3) {
                    val p0 = parts[0].toIntOrNull()
                    val p1 = parts[1].toIntOrNull()
                    val p2 = parts[2].toIntOrNull()
                    if (p0 != null && p1 != null && p2 != null) {
                        if (p0 > 1000) {
                            return String.format(Locale.US, "%04d-%02d-%02d", p0, p1, p2)
                        } else if (p2 > 1000) {
                            return String.format(Locale.US, "%04d-%02d-%02d", p2, p1, p0)
                        }
                    }
                }
            }
        }
        return clean
    }

    fun obtenerHoraFormateada(movimiento: Movimiento): String {
        val fechaCompleta = if (movimiento.fecha.isNotBlank()) movimiento.fecha else movimiento.fechaCreacion
        return try {
            val ts = fechaCompleta.trim().toLongOrNull()
            if (ts != null && ts > 100000000000L) {
                return formatHora.format(Date(ts))
            }
            if (fechaCompleta.contains(" ")) {
                val partes = fechaCompleta.split(" ")
                val horaPartes = partes[1].split(":")
                "${horaPartes[0]}:${horaPartes[1]} hrs"
            } else {
                "12:00 hrs"
            }
        } catch (e: Exception) {
            "12:00 hrs"
        }
    }

    fun procesarGrupos(movimientos: List<Movimiento>): List<MovimientoListItem> {
        val result = mutableListOf<MovimientoListItem>()
        if (movimientos.isEmpty()) return result

        val movimientosOrdenados = movimientos.sortedWith(
            compareByDescending<Movimiento> { NotasDatabaseHelper.obtenerTimestampParaOrdenamiento(it) }
                .thenByDescending { it.id }
        )

        val sdfParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calHoy = Calendar.getInstance()
        val strHoy = sdfParser.format(calHoy.time)

        val calAyer = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val strAyer = sdfParser.format(calAyer.time)

        val sdfHeader = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

        val agrupados = LinkedHashMap<String, MutableList<Movimiento>>()
        for (mov in movimientosOrdenados) {
            val fechaEfectiva = if (mov.fecha.isNotBlank()) mov.fecha else mov.fechaCreacion
            val keyFecha = normalizarFechaClave(fechaEfectiva)
            agrupados.getOrPut(keyFecha) { mutableListOf() }.add(mov)
        }

        for ((fechaKey, lista) in agrupados) {
            val tituloHeader = when (fechaKey) {
                strHoy -> {
                    try {
                        val d = sdfParser.parse(fechaKey)
                        "HOY • ${sdfHeader.format(d ?: Date()).uppercase()}"
                    } catch (e: Exception) {
                        "HOY"
                    }
                }
                strAyer -> {
                    try {
                        val d = sdfParser.parse(fechaKey)
                        "AYER • ${sdfHeader.format(d ?: Date()).uppercase()}"
                    } catch (e: Exception) {
                        "AYER"
                    }
                }
                else -> {
                    try {
                        val d = sdfParser.parse(fechaKey)
                        sdfHeader.format(d ?: Date()).uppercase()
                    } catch (e: Exception) {
                        fechaKey
                    }
                }
            }

            val conteoTexto = if (lista.size == 1) "1 Movimiento" else "${lista.size} Movimientos"
            result.add(MovimientoListItem.Header(tituloHeader, conteoTexto))

            for (mov in lista) {
                result.add(MovimientoListItem.Item(mov))
            }
        }
        return result
    }
}
