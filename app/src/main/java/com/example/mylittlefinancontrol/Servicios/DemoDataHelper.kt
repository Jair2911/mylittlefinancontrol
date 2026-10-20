package com.example.mylittlefinancontrol.Servicios

import android.content.Context
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.Modelos.Ahorro
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

object DemoDataHelper {

    fun cargarDatosPrueba(context: Context, db: NotasDatabaseHelper) {
        val sqliteDb = db.writableDatabase
        sqliteDb.beginTransaction()
        try {
            // Limpiamos los datos anteriores dentro de la misma transacción
            db.clearAllData()

            val cal = Calendar.getInstance()
            val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

            fun fechaOffsetDias(diasOffset: Int, hora: Int, minuto: Int): String {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, diasOffset)
                c.set(Calendar.HOUR_OF_DAY, hora)
                c.set(Calendar.MINUTE, minuto)
                return sdf.format(c.time)
            }

            // 1. GASTOS FIJOS
            db.insertGastoFijo(GastoFijo("", "Arriendo Apartamento", "Vivienda", 850000.0, "05", "Mensual", mesAnioActual, ""))
            db.insertGastoFijo(GastoFijo("", "Internet Fibra Óptica 500MB", "Telecom", 115000.0, "10", "Mensual", mesAnioActual, ""))
            db.insertGastoFijo(GastoFijo("", "Servicio de Energía y Agua", "Servicios", 160000.0, "15", "Quincenal", mesAnioActual, ""))
            db.insertGastoFijo(GastoFijo("", "Plan Móvil Ilimitado", "Telecom", 55000.0, "15", "Mensual", "", ""))
            db.insertGastoFijo(GastoFijo("", "Suscripción Netflix & Spotify", "Streaming", 48000.0, "20", "Mensual", "", ""))
            db.insertGastoFijo(GastoFijo("", "Cuota Especialización", "Educación", 320000.0, "30", "Mensual", "", ""))

            // 2. DEUDAS
            db.insertDeuda(Deuda("", "Tarjeta de Crédito", "Tarjeta Visa Platinum", "Cuotas computador y compras hogar", 1800000.0, 750000.0, "2026-01-15", "2026-12-15"))
            db.insertDeuda(Deuda("", "Préstamo Bancario", "Crédito Vehicular Bancolombia", "Financiamiento automóvil cuotas fijas", 6500000.0, 2600000.0, "2025-06-01", "2027-06-01"))
            db.insertDeuda(Deuda("", "Préstamo Personal", "Préstamo Personal Familiar", "Préstamo personal sin intereses", 500000.0, 200000.0, "2026-04-01", "2026-10-01"))

            // 3. METAS DE AHORRO
            db.insertAhorro(Ahorro("", "Fondo de Emergencia", "Emergencia", 1250000.0, 3000000.0, "Dic 2026", "shield", "Colchón de seguridad"))
            db.insertAhorro(Ahorro("", "Viaje Vacaciones Playa", "Viajes", 850000.0, 2000000.0, "Nov 2026", "cloud", "Vuelos y hotel"))
            db.insertAhorro(Ahorro("", "Renovación Computador", "Tecnología", 400000.0, 1500000.0, "Ene 2027", "router", "Laptop para trabajo"))

            // 4. MOVIMIENTOS HISTÓRICOS Y ACTUALES
            db.insertNota(Movimiento(0, "PAGO NÓMINA QUINCENA", 2800000, "INGRESO", fechaOffsetDias(0, 8, 30)))
            db.insertNota(Movimiento(0, "SUPERMERCADO ÉXITO", 245000, "GASTO", fechaOffsetDias(0, 14, 15)))
            db.insertNota(Movimiento(0, "CAFÉ Y MERIENDA JUAN VALDEZ", 18500, "GASTO", fechaOffsetDias(0, 17, 40)))
            db.insertNota(Movimiento(0, "PAGO: ARRIENDO APARTAMENTO", 850000, "GASTO FIJO", fechaOffsetDias(-1, 10, 0), 1, 0))
            db.insertNota(Movimiento(0, "SUPERMERCADO CARULLA EXPRESS", 68000, "GASTO", fechaOffsetDias(-1, 15, 30)))
            db.insertNota(Movimiento(0, "PAGO: INTERNET FIBRA ÓPTICA", 115000, "GASTO FIJO", fechaOffsetDias(-2, 11, 20), 2, 0))
            db.insertNota(Movimiento(0, "HONORARIOS PROYECTO FREELANCE", 650000, "INGRESO", fechaOffsetDias(-2, 16, 45)))
            db.insertNota(Movimiento(0, "COMIDA RESTAURANTE ITALIANO", 78000, "GASTO", fechaOffsetDias(-3, 20, 10)))
            db.insertNota(Movimiento(0, "PAGO DEUDA: TARJETA VISA", 250000, "PAGO DEUDA", fechaOffsetDias(-3, 9, 15)))
            db.insertNota(Movimiento(0, "GASOLINA Y TRANSPORTE ESTACIÓN", 65000, "GASTO", fechaOffsetDias(-4, 17, 30)))
            db.insertNota(Movimiento(0, "PAGO: SERVICIO ENERGÍA Y AGUA", 160000, "GASTO FIJO", fechaOffsetDias(-4, 12, 0), 3, 1))
            db.insertNota(Movimiento(0, "COMPRA FARMACIA Y SALUD", 42000, "GASTO", fechaOffsetDias(-5, 18, 20)))
            db.insertNota(Movimiento(0, "PAGO DEUDA: CRÉDITO VEHICULAR", 350000, "PAGO DEUDA", fechaOffsetDias(-6, 10, 30)))
            db.insertNota(Movimiento(0, "VENTA ARTÍCULOS Y RETORNO", 120000, "INGRESO", fechaOffsetDias(-7, 14, 0)))
            db.insertNota(Movimiento(0, "PAGO: SUSCRIPCIÓN NETFLIX & SPOTIFY", 48000, "GASTO FIJO", fechaOffsetDias(-8, 9, 0), 5, 0))
            db.insertNota(Movimiento(0, "SUPERMERCADO D1 DESPENSA", 95000, "GASTO", fechaOffsetDias(-9, 16, 15)))

            sqliteDb.setTransactionSuccessful()
        } finally {
            sqliteDb.endTransaction()
        }
    }

    fun limpiarTodosLosDatos(context: Context, db: NotasDatabaseHelper) {
        db.clearAllData()
    }
}
