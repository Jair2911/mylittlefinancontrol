package com.example.mylittlefinancontrol.Servicios

import com.example.mylittlefinancontrol.Modelos.CategoriaGastoItem
import java.text.NumberFormat
import java.util.Locale

data class DiagnosticoFinanciero(
    val puntajeSalud: Int, // 0 a 100
    val estadoGeneral: String, // "Excelente", "Saludable", "En Riesgo", "Crítico"
    val resumenEjecutivo: String,
    val puntosClave: List<String>,
    val recomendacionAccionable: String,
    val gastoPromedioDiario: Double
)

object AnalizadorFinancieroLocal {

    private val formatoMoneda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    /**
     * Genera un diagnóstico financiero determinista y exhaustivo 100% offline.
     */
    fun generarDiagnostico(
        periodo: String,
        ingresosTotales: Double,
        gastosTotales: Double,
        gastosPeriodoAnterior: Double,
        categorias: List<CategoriaGastoItem>,
        totalGastosFijos: Double,
        totalCuotasDeudas: Double
    ): DiagnosticoFinanciero {
        val balance = ingresosTotales - gastosTotales
        val puntosClave = mutableListOf<String>()

        val nombrePeriodo = when (periodo.uppercase(Locale.ROOT)) {
            "SEMANAL" -> "esta semana"
            "MENSUAL" -> "este mes"
            "SEMESTRAL" -> "este semestre"
            "ANUAL" -> "este año"
            else -> "este periodo"
        }

        // --- REGLA 1: BALANCE Y TASA DE AHORRO ---
        var tasaAhorro = 0.0
        if (ingresosTotales > 0) {
            tasaAhorro = ((ingresosTotales - gastosTotales) / ingresosTotales) * 100.0
            if (gastosTotales > ingresosTotales) {
                val sobregiro = gastosTotales - ingresosTotales
                puntosClave.add("⚠️ Alerta de déficit: Tus gastos superan tus ingresos en ${formatoMoneda.format(sobregiro)}.")
            } else if (tasaAhorro >= 20.0) {
                puntosClave.add("🎉 Tasa de ahorro sobresaliente: Has conservado el ${String.format(Locale.US, "%.1f", tasaAhorro)}% de tus ingresos durante $nombrePeriodo.")
            } else {
                puntosClave.add("Tasa de ahorro moderada: Retienes el ${String.format(Locale.US, "%.1f", tasaAhorro)}% de tus ingresos.")
            }
        } else if (gastosTotales > 0) {
            puntosClave.add("⚠️ Registras salidas por ${formatoMoneda.format(gastosTotales)} sin ingresos reportados aún en $nombrePeriodo.")
        }

        // --- REGLA 2: CATEGORÍA DOMINANTE Y RECOMENDACIÓN ---
        var recomendacion = ""
        val categoriaDominante = categorias.maxByOrNull { it.totalMonto }
        if (categoriaDominante != null && gastosTotales > 0) {
            val pctCat = (categoriaDominante.totalMonto / gastosTotales) * 100.0
            if (pctCat >= 35.0) {
                puntosClave.add(
                    "Tu mayor concentración de dinero está en ${categoriaDominante.nombreCategoria} (${formatoMoneda.format(categoriaDominante.totalMonto)}, equivalente al ${String.format(Locale.US, "%.1f", pctCat)}% de tus salidas)."
                )
            }

            val nombreCatLower = categoriaDominante.nombreCategoria.lowercase(Locale.ROOT)
            val esGastoFlexible = nombreCatLower.contains("comida") ||
                    nombreCatLower.contains("ocio") ||
                    nombreCatLower.contains("cine") ||
                    nombreCatLower.contains("películas") ||
                    nombreCatLower.contains("peliculas") ||
                    nombreCatLower.contains("hobbies") ||
                    nombreCatLower.contains("tienda") ||
                    nombreCatLower.contains("restaurante") ||
                    nombreCatLower.contains("streaming")

            if (esGastoFlexible) {
                val ahorroPotencial = categoriaDominante.totalMonto * 0.15
                recomendacion = "Reduce un 15% en '${categoriaDominante.nombreCategoria}' el próximo periodo para liberar ${formatoMoneda.format(ahorroPotencial)} de ahorro directo."
            } else {
                recomendacion = "Revisa los movimientos de '${categoriaDominante.nombreCategoria}' (${formatoMoneda.format(categoriaDominante.totalMonto)}) para negociar tarifas o evitar gastos hormiga."
            }
        }

        // --- REGLA 3: COMPARATIVA TEMPORAL ---
        if (gastosPeriodoAnterior > 0 && gastosTotales > 0) {
            val variacion = ((gastosTotales - gastosPeriodoAnterior) / gastosPeriodoAnterior) * 100.0
            val comparativoTexto = when (periodo.uppercase(Locale.ROOT)) {
                "SEMANAL" -> "la semana anterior"
                "MENSUAL" -> "el mes anterior"
                "SEMESTRAL" -> "el semestre anterior"
                "ANUAL" -> "el año anterior"
                else -> "el periodo anterior"
            }

            if (variacion > 0) {
                puntosClave.add("📈 Tus gastos aumentaron un ${String.format(Locale.US, "%.1f", variacion)}% respecto a $comparativoTexto (${formatoMoneda.format(gastosPeriodoAnterior)}).")
            } else if (variacion < 0) {
                puntosClave.add("📉 Lograste reducir tus gastos en un ${String.format(Locale.US, "%.1f", Math.abs(variacion))}% en comparación con $comparativoTexto.")
            } else {
                puntosClave.add("Tus gastos se mantuvieron exactamente iguales respecto a $comparativoTexto.")
            }
        }

        // --- REGLA 4: CARGA FIJA Y ENDEUDAMIENTO ---
        val cargaComprometida = totalGastosFijos + totalCuotasDeudas
        if (ingresosTotales > 0 && cargaComprometida > 0) {
            val pctCarga = (cargaComprometida / ingresosTotales) * 100.0
            if (pctCarga > 50.0) {
                puntosClave.add("⚠️ Rigidez financiera: Gastos fijos y cuotas de deuda comprometen el ${String.format(Locale.US, "%.1f", pctCarga)}% de tus ingresos, superando el límite seguro del 50%.")
                if (totalCuotasDeudas > 0 && recomendacion.isBlank()) {
                    recomendacion = "Prioriza el abono a capital de tus deudas activas para aliviar tu flujo de caja mensual en ${formatoMoneda.format(totalCuotasDeudas)}."
                }
            } else {
                puntosClave.add("Compromisos fijos bajo control: Deudas y gastos fijos representan el ${String.format(Locale.US, "%.1f", pctCarga)}% de tus ingresos.")
            }
        }

        // --- REGLA 5: RITMO DE CONSUMO DIARIO ---
        val diasPeriodo = when (periodo.uppercase(Locale.ROOT)) {
            "SEMANAL" -> 7
            "MENSUAL" -> 30
            "SEMESTRAL" -> 180
            "ANUAL" -> 365
            else -> 30
        }
        val gastoDiario = if (diasPeriodo > 0) gastosTotales / diasPeriodo else 0.0
        puntosClave.add("⏱️ Ritmo de consumo: Tu gasto promedio diario es de ${formatoMoneda.format(gastoDiario)} / día.")

        // Recomendación fallback si no se asignó ninguna
        if (recomendacion.isBlank()) {
            recomendacion = if (balance >= 0) {
                "Destina al menos el 50% de tu balance a favor (${formatoMoneda.format(balance)}) a tus metas de ahorro o fondo de emergencia."
            } else {
                "Pausa compras no esenciales para estabilizar tu flujo de caja y evitar financiamiento costoso con tarjeta de crédito."
            }
        }

        // --- CÁLCULO DEL PUNTAJE DE SALUD FINANCIERA (0 A 100) ---
        var score = 100

        if (ingresosTotales > 0) {
            when {
                tasaAhorro < 0 -> {
                    val def = Math.abs(tasaAhorro)
                    score -= when {
                        def > 40 -> 40
                        def > 20 -> 30
                        else -> 20
                    }
                }
                tasaAhorro < 10 -> score -= 15
                tasaAhorro < 20 -> score -= 5
            }

            if (cargaComprometida > 0) {
                val pctCarga = (cargaComprometida / ingresosTotales) * 100.0
                if (pctCarga > 60.0) score -= 20
                else if (pctCarga > 50.0) score -= 10
            }
        } else if (gastosTotales > 0) {
            score = 35
        } else {
            score = 75
        }

        if (categoriaDominante != null && gastosTotales > 0) {
            val pctCat = (categoriaDominante.totalMonto / gastosTotales) * 100.0
            if (pctCat > 55.0) score -= 15
            else if (pctCat > 40.0) score -= 8
        }

        if (gastosPeriodoAnterior > 0 && gastosTotales > 0) {
            val varPct = ((gastosTotales - gastosPeriodoAnterior) / gastosPeriodoAnterior) * 100.0
            if (varPct > 30.0) score -= 10
            else if (varPct < -5.0) score = (score + 5).coerceAtMost(100)
        }

        val puntajeFinal = score.coerceIn(0, 100)

        val estadoGeneral = when {
            puntajeFinal >= 80 -> "Excelente"
            puntajeFinal >= 65 -> "Saludable"
            puntajeFinal >= 45 -> "En Riesgo"
            else -> "Crítico"
        }

        // --- RESUMEN EJECUTIVO DETERMINISTA ---
        val ingFmt = formatoMoneda.format(ingresosTotales)
        val gastFmt = formatoMoneda.format(gastosTotales)
        val balFmt = formatoMoneda.format(balance)

        val resumenEjecutivo = if (ingresosTotales > 0) {
            if (balance >= 0) {
                "Durante $nombrePeriodo registraste ingresos por $ingFmt y gastos por $gastFmt, logrando un balance favorable de $balFmt con una tasa de ahorro del ${String.format(Locale.US, "%.1f", tasaAhorro)}%."
            } else {
                "Durante $nombrePeriodo registraste ingresos por $ingFmt y gastos por $gastFmt, generando un déficit financiero de ${formatoMoneda.format(Math.abs(balance))}."
            }
        } else if (gastosTotales > 0) {
            "Durante $nombrePeriodo acumulaste salidas por $gastFmt sin registrar ingresos reportados, dejando un saldo en contra de $gastFmt."
        } else {
            "No se registran movimientos financieros durante $nombrePeriodo."
        }

        return DiagnosticoFinanciero(
            puntajeSalud = puntajeFinal,
            estadoGeneral = estadoGeneral,
            resumenEjecutivo = resumenEjecutivo,
            puntosClave = puntosClave.take(4),
            recomendacionAccionable = recomendacion,
            gastoPromedioDiario = gastoDiario
        )
    }
}
