package com.example.mylittlefinancontrol

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mylittlefinancontrol.Modelos.Ahorro
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo

class NotasDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "finanzas.db"
        private const val DATABASE_VERSION = 8

        private const val TABLE_MOVIMIENTO = "movimiento"
        private const val COLUMN_MOV_ID = "id"
        private const val COLUMN_MOV_DESC = "descripcion"
        private const val COLUMN_MOV_VALOR = "valor"
        private const val COLUMN_MOV_TIPO = "tipo_reg"
        private const val COLUMN_MOV_FECHA = "fecha"
        private const val COLUMN_MOV_GF_ID = "gasto_fijo_id"
        private const val COLUMN_MOV_Q_PAGADA = "quincena_pagada"

        private const val TABLE_GASTOS_FIJOS = "gastos_fijos"
        private const val COLUMN_GF_ID = "id"
        private const val COLUMN_GF_CONCEPTO = "concepto"
        private const val COLUMN_GF_CATEGORIA = "categoria"
        private const val COLUMN_GF_MONTO = "monto"
        private const val COLUMN_GF_DIA = "diaPago"
        private const val COLUMN_GF_PERIODO = "periodo"
        private const val COLUMN_GF_U_PAGO_Q1 = "ultimo_pago_q1"
        private const val COLUMN_GF_U_PAGO_Q2 = "ultimo_pago_q2"

        private const val TABLE_DEUDAS = "deudas"
        private const val COLUMN_D_ID = "id"
        private const val COLUMN_D_TIPO = "tipo"
        private const val COLUMN_D_CONCEPTO = "concepto"
        private const val COLUMN_D_DESC = "descripcion"
        private const val COLUMN_D_TOTAL = "totalDeuda"
        private const val COLUMN_D_PAGADO = "totalPagado"
        private const val COLUMN_D_FECHA_INI = "fecha_inicio"
        private const val COLUMN_D_FECHA_FIN = "fecha_fin"

        private const val TABLE_AHORROS = "ahorros"
        private const val COLUMN_A_ID = "id"
        private const val COLUMN_A_TITULO = "titulo"
        private const val COLUMN_A_CATEGORIA = "categoria"
        private const val COLUMN_A_MONTO_ACTUAL = "monto_actual"
        private const val COLUMN_A_MONTO_OBJETIVO = "monto_objetivo"
        private const val COLUMN_A_FECHA_META = "fecha_meta"
        private const val COLUMN_A_ICONO = "icono"
        private const val COLUMN_A_DESC = "descripcion"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_MOVIMIENTO ($COLUMN_MOV_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_MOV_DESC TEXT, $COLUMN_MOV_VALOR INTEGER, $COLUMN_MOV_TIPO TEXT, $COLUMN_MOV_FECHA TEXT, $COLUMN_MOV_GF_ID INTEGER, $COLUMN_MOV_Q_PAGADA INTEGER)")
        db?.execSQL("CREATE TABLE $TABLE_GASTOS_FIJOS ($COLUMN_GF_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_GF_CONCEPTO TEXT, $COLUMN_GF_CATEGORIA TEXT, $COLUMN_GF_MONTO REAL, $COLUMN_GF_DIA TEXT, $COLUMN_GF_PERIODO TEXT, $COLUMN_GF_U_PAGO_Q1 TEXT, $COLUMN_GF_U_PAGO_Q2 TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_DEUDAS ($COLUMN_D_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_D_TIPO TEXT, $COLUMN_D_CONCEPTO TEXT, $COLUMN_D_DESC TEXT, $COLUMN_D_TOTAL REAL, $COLUMN_D_PAGADO REAL, $COLUMN_D_FECHA_INI TEXT, $COLUMN_D_FECHA_FIN TEXT)")
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_AHORROS ($COLUMN_A_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_A_TITULO TEXT, $COLUMN_A_CATEGORIA TEXT, $COLUMN_A_MONTO_ACTUAL REAL, $COLUMN_A_MONTO_OBJETIVO REAL, $COLUMN_A_FECHA_META TEXT, $COLUMN_A_ICONO TEXT, $COLUMN_A_DESC TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 7) {
            db?.execSQL("ALTER TABLE $TABLE_MOVIMIENTO ADD COLUMN $COLUMN_MOV_GF_ID INTEGER")
            db?.execSQL("ALTER TABLE $TABLE_MOVIMIENTO ADD COLUMN $COLUMN_MOV_Q_PAGADA INTEGER")
            
            db?.execSQL("ALTER TABLE $TABLE_GASTOS_FIJOS ADD COLUMN $COLUMN_GF_U_PAGO_Q1 TEXT")
            db?.execSQL("ALTER TABLE $TABLE_GASTOS_FIJOS ADD COLUMN $COLUMN_GF_U_PAGO_Q2 TEXT")
            
            db?.execSQL("ALTER TABLE $TABLE_DEUDAS ADD COLUMN $COLUMN_D_FECHA_INI TEXT")
            db?.execSQL("ALTER TABLE $TABLE_DEUDAS ADD COLUMN $COLUMN_D_FECHA_FIN TEXT")
        }
        if (oldVersion < 8) {
            db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_AHORROS ($COLUMN_A_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_A_TITULO TEXT, $COLUMN_A_CATEGORIA TEXT, $COLUMN_A_MONTO_ACTUAL REAL, $COLUMN_A_MONTO_OBJETIVO REAL, $COLUMN_A_FECHA_META TEXT, $COLUMN_A_ICONO TEXT, $COLUMN_A_DESC TEXT)")
        }
    }

    // --- MÉTODOS MOVIMIENTOS ---
    fun insertNota(mov: Movimiento) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_MOV_DESC, mov.descripcion)
            put(COLUMN_MOV_VALOR, mov.valor)
            put(COLUMN_MOV_TIPO, mov.tipo_reg)
            put(COLUMN_MOV_FECHA, mov.fecha)
            put(COLUMN_MOV_GF_ID, mov.gastoFijoId)
            put(COLUMN_MOV_Q_PAGADA, mov.quincenaPagada)
        }
        db.insert(TABLE_MOVIMIENTO, null, v)
        db.close()
    }

    fun getAllNotas(): List<Movimiento> {
        val lista = mutableListOf<Movimiento>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MOVIMIENTO ORDER BY $COLUMN_MOV_ID DESC", null)
        while (cursor.moveToNext()) {
            lista.add(Movimiento(
                cursor.getInt(0), 
                cursor.getString(1), 
                cursor.getInt(2), 
                cursor.getString(3), 
                cursor.getString(4),
                if (cursor.isNull(5)) null else cursor.getInt(5),
                if (cursor.isNull(6)) null else cursor.getInt(6)
            ))
        }
        cursor.close()
        return lista
    }

    fun getIdMovimiento(id: Int): Movimiento {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MOVIMIENTO WHERE $COLUMN_MOV_ID = ?", arrayOf(id.toString()))
        cursor.moveToFirst()
        val mov = Movimiento(
            cursor.getInt(0), 
            cursor.getString(1), 
            cursor.getInt(2), 
            cursor.getString(3), 
            cursor.getString(4),
            if (cursor.isNull(5)) null else cursor.getInt(5),
            if (cursor.isNull(6)) null else cursor.getInt(6)
        )
        cursor.close()
        return mov
    }

    fun updateMovimiento(mov: Movimiento) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_MOV_DESC, mov.descripcion)
            put(COLUMN_MOV_VALOR, mov.valor)
            put(COLUMN_MOV_TIPO, mov.tipo_reg)
            put(COLUMN_MOV_FECHA, mov.fecha)
            put(COLUMN_MOV_GF_ID, mov.gastoFijoId)
            put(COLUMN_MOV_Q_PAGADA, mov.quincenaPagada)
        }
        db.update(TABLE_MOVIMIENTO, v, "$COLUMN_MOV_ID = ?", arrayOf(mov.id.toString()))
        db.close()
    }

    fun deleteMovimiento(id: Int): Int {
        val db = writableDatabase
        val result = db.delete(TABLE_MOVIMIENTO, "$COLUMN_MOV_ID = ?", arrayOf(id.toString()))
        db.close()
        return result
    }

    // --- MÉTODOS GASTOS FIJOS ---
    fun insertGastoFijo(gf: GastoFijo) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_GF_CONCEPTO, gf.concepto); put(COLUMN_GF_CATEGORIA, gf.categoria)
            put(COLUMN_GF_MONTO, gf.monto); put(COLUMN_GF_DIA, gf.diaPago); put(COLUMN_GF_PERIODO, gf.periodo)
            put(COLUMN_GF_U_PAGO_Q1, gf.ultimoPagoQ1); put(COLUMN_GF_U_PAGO_Q2, gf.ultimoPagoQ2)
        }
        db.insert(TABLE_GASTOS_FIJOS, null, v)
        db.close()
    }

    fun updateGastoFijo(gf: GastoFijo) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_GF_CONCEPTO, gf.concepto); put(COLUMN_GF_CATEGORIA, gf.categoria)
            put(COLUMN_GF_MONTO, gf.monto); put(COLUMN_GF_DIA, gf.diaPago); put(COLUMN_GF_PERIODO, gf.periodo)
            put(COLUMN_GF_U_PAGO_Q1, gf.ultimoPagoQ1); put(COLUMN_GF_U_PAGO_Q2, gf.ultimoPagoQ2)
        }
        db.update(TABLE_GASTOS_FIJOS, v, "$COLUMN_GF_ID = ?", arrayOf(gf.id))
        db.close()
    }

    fun getAllGastosFijos(): List<GastoFijo> {
        val lista = mutableListOf<GastoFijo>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_GASTOS_FIJOS", null)
        while (cursor.moveToNext()) {
            lista.add(GastoFijo(
                cursor.getInt(0).toString(), 
                cursor.getString(1), 
                cursor.getString(2), 
                cursor.getDouble(3), 
                cursor.getString(4), 
                cursor.getString(5),
                cursor.getString(6) ?: "",
                cursor.getString(7) ?: ""
            ))
        }
        cursor.close()
        return lista
    }

    fun deleteGastoFijo(id: String) {
        val db = writableDatabase
        db.delete(TABLE_GASTOS_FIJOS, "$COLUMN_GF_ID = ?", arrayOf(id))
        db.close()
    }

    // --- MÉTODOS DEUDAS ---
    fun insertDeuda(d: Deuda) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_D_TIPO, d.tipo); put(COLUMN_D_CONCEPTO, d.concepto)
            put(COLUMN_D_DESC, d.descripcion); put(COLUMN_D_TOTAL, d.totalDeuda); put(COLUMN_D_PAGADO, d.totalPagado)
            put(COLUMN_D_FECHA_INI, d.fechaInicio); put(COLUMN_D_FECHA_FIN, d.fechaFin)
        }
        db.insert(TABLE_DEUDAS, null, v)
        db.close()
    }

    fun updateDeuda(d: Deuda) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_D_TIPO, d.tipo); put(COLUMN_D_CONCEPTO, d.concepto)
            put(COLUMN_D_DESC, d.descripcion); put(COLUMN_D_TOTAL, d.totalDeuda); put(COLUMN_D_PAGADO, d.totalPagado)
            put(COLUMN_D_FECHA_INI, d.fechaInicio); put(COLUMN_D_FECHA_FIN, d.fechaFin)
        }
        db.update(TABLE_DEUDAS, v, "$COLUMN_D_ID = ?", arrayOf(d.id))
        db.close()
    }

    fun getAllDeudas(): List<Deuda> {
        val lista = mutableListOf<Deuda>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DEUDAS", null)
        while (cursor.moveToNext()) {
            lista.add(Deuda(
                cursor.getInt(0).toString(), 
                cursor.getString(1), 
                cursor.getString(2), 
                cursor.getString(3), 
                cursor.getDouble(4), 
                cursor.getDouble(5),
                cursor.getString(6) ?: "",
                cursor.getString(7) ?: ""
            ))
        }
        cursor.close()
        return lista
    }

    fun deleteDeuda(id: String) {
        val db = writableDatabase
        db.delete(TABLE_DEUDAS, "$COLUMN_D_ID = ?", arrayOf(id))
        db.close()
    }

    // --- MÉTODOS AHORROS ---
    fun insertAhorro(a: Ahorro) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_A_TITULO, a.titulo)
            put(COLUMN_A_CATEGORIA, a.categoria)
            put(COLUMN_A_MONTO_ACTUAL, a.montoActual)
            put(COLUMN_A_MONTO_OBJETIVO, a.montoObjetivo)
            put(COLUMN_A_FECHA_META, a.fechaMeta)
            put(COLUMN_A_ICONO, a.icono)
            put(COLUMN_A_DESC, a.descripcion)
        }
        db.insert(TABLE_AHORROS, null, v)
        db.close()
    }

    fun updateAhorro(a: Ahorro) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_A_TITULO, a.titulo)
            put(COLUMN_A_CATEGORIA, a.categoria)
            put(COLUMN_A_MONTO_ACTUAL, a.montoActual)
            put(COLUMN_A_MONTO_OBJETIVO, a.montoObjetivo)
            put(COLUMN_A_FECHA_META, a.fechaMeta)
            put(COLUMN_A_ICONO, a.icono)
            put(COLUMN_A_DESC, a.descripcion)
        }
        db.update(TABLE_AHORROS, v, "$COLUMN_A_ID = ?", arrayOf(a.id))
        db.close()
    }

    fun getAllAhorros(): List<Ahorro> {
        val lista = mutableListOf<Ahorro>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_AHORROS ORDER BY $COLUMN_A_ID DESC", null)
        while (cursor.moveToNext()) {
            lista.add(Ahorro(
                cursor.getInt(0).toString(),
                cursor.getString(1) ?: "",
                cursor.getString(2) ?: "Emergencia",
                cursor.getDouble(3),
                cursor.getDouble(4),
                cursor.getString(5) ?: "",
                cursor.getString(6) ?: "savings",
                cursor.getString(7) ?: ""
            ))
        }
        cursor.close()
        return lista
    }

    fun deleteAhorro(id: String) {
        val db = writableDatabase
        db.delete(TABLE_AHORROS, "$COLUMN_A_ID = ?", arrayOf(id))
        db.close()
    }

    fun getResumenFinanciero(): com.example.mylittlefinancontrol.Modelos.ResumenFinanciero {
        val db = readableDatabase
        
        // 1. Balance Total (Ingresos - Gastos de movimientos)
        var ingresos = 0.0
        var gastos = 0.0
        val cursorMov = db.rawQuery("SELECT $COLUMN_MOV_VALOR, $COLUMN_MOV_TIPO FROM $TABLE_MOVIMIENTO", null)
        while (cursorMov.moveToNext()) {
            val valor = cursorMov.getDouble(0)
            val tipo = cursorMov.getString(1)
            if (tipo.equals("Ingreso", ignoreCase = true)) {
                ingresos += valor
            } else {
                gastos += valor
            }
        }
        cursorMov.close()
        val balance = ingresos - gastos

        // 2. Total Gastos Fijos
        var totalGF = 0.0
        val cursorGF = db.rawQuery("SELECT SUM($COLUMN_GF_MONTO) FROM $TABLE_GASTOS_FIJOS", null)
        if (cursorGF.moveToFirst()) {
            totalGF = cursorGF.getDouble(0)
        }
        cursorGF.close()

        // 3. Total Deudas Pendientes (Total - Pagado)
        var totalDeudas = 0.0
        val cursorD = db.rawQuery("SELECT SUM($COLUMN_D_TOTAL - $COLUMN_D_PAGADO) FROM $TABLE_DEUDAS", null)
        if (cursorD.moveToFirst()) {
            totalDeudas = cursorD.getDouble(0)
        }
        cursorD.close()

        return com.example.mylittlefinancontrol.Modelos.ResumenFinanciero(balance, totalGF, totalDeudas)
    }
}
