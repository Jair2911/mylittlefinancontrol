package com.example.mylittlefinancontrol

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo

class NotasDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "finanzas.db"
        private const val DATABASE_VERSION = 6

        private const val TABLE_MOVIMIENTO = "movimiento"
        private const val COLUMN_MOV_ID = "id"
        private const val COLUMN_MOV_DESC = "descripcion"
        private const val COLUMN_MOV_VALOR = "valor"
        private const val COLUMN_MOV_TIPO = "tipo_reg"
        private const val COLUMN_MOV_FECHA = "fecha"

        private const val TABLE_GASTOS_FIJOS = "gastos_fijos"
        private const val COLUMN_GF_ID = "id"
        private const val COLUMN_GF_CONCEPTO = "concepto"
        private const val COLUMN_GF_CATEGORIA = "categoria"
        private const val COLUMN_GF_MONTO = "monto"
        private const val COLUMN_GF_DIA = "diaPago"
        private const val COLUMN_GF_PERIODO = "periodo"

        private const val TABLE_DEUDAS = "deudas"
        private const val COLUMN_D_ID = "id"
        private const val COLUMN_D_TIPO = "tipo"
        private const val COLUMN_D_CONCEPTO = "concepto"
        private const val COLUMN_D_DESC = "descripcion"
        private const val COLUMN_D_TOTAL = "totalDeuda"
        private const val COLUMN_D_PAGADO = "totalPagado"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_MOVIMIENTO ($COLUMN_MOV_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_MOV_DESC TEXT, $COLUMN_MOV_VALOR INTEGER, $COLUMN_MOV_TIPO TEXT, $COLUMN_MOV_FECHA TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_GASTOS_FIJOS ($COLUMN_GF_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_GF_CONCEPTO TEXT, $COLUMN_GF_CATEGORIA TEXT, $COLUMN_GF_MONTO REAL, $COLUMN_GF_DIA TEXT, $COLUMN_GF_PERIODO TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_DEUDAS ($COLUMN_D_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_D_TIPO TEXT, $COLUMN_D_CONCEPTO TEXT, $COLUMN_D_DESC TEXT, $COLUMN_D_TOTAL REAL, $COLUMN_D_PAGADO REAL)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MOVIMIENTO")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GASTOS_FIJOS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_DEUDAS")
        onCreate(db)
    }

    // --- MÉTODOS MOVIMIENTOS ---
    fun insertNota(mov: Movimiento) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_MOV_DESC, mov.descripcion)
            put(COLUMN_MOV_VALOR, mov.valor)
            put(COLUMN_MOV_TIPO, mov.tipo_reg)
            put(COLUMN_MOV_FECHA, mov.fecha)
        }
        db.insert(TABLE_MOVIMIENTO, null, v)
        db.close()
    }

    fun getAllNotas(): List<Movimiento> {
        val lista = mutableListOf<Movimiento>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MOVIMIENTO ORDER BY $COLUMN_MOV_ID DESC", null)
        while (cursor.moveToNext()) {
            lista.add(Movimiento(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getString(3), cursor.getString(4)))
        }
        cursor.close()
        return lista
    }

    fun getIdMovimiento(id: Int): Movimiento {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MOVIMIENTO WHERE $COLUMN_MOV_ID = ?", arrayOf(id.toString()))
        cursor.moveToFirst()
        val mov = Movimiento(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getString(3), cursor.getString(4))
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
        }
        db.insert(TABLE_GASTOS_FIJOS, null, v)
        db.close()
    }

    fun updateGastoFijo(gf: GastoFijo) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_GF_CONCEPTO, gf.concepto); put(COLUMN_GF_CATEGORIA, gf.categoria)
            put(COLUMN_GF_MONTO, gf.monto); put(COLUMN_GF_DIA, gf.diaPago); put(COLUMN_GF_PERIODO, gf.periodo)
        }
        db.update(TABLE_GASTOS_FIJOS, v, "$COLUMN_GF_ID = ?", arrayOf(gf.id))
        db.close()
    }

    fun getAllGastosFijos(): List<GastoFijo> {
        val lista = mutableListOf<GastoFijo>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_GASTOS_FIJOS", null)
        while (cursor.moveToNext()) {
            lista.add(GastoFijo(cursor.getInt(0).toString(), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getString(4), cursor.getString(5)))
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
        }
        db.insert(TABLE_DEUDAS, null, v)
        db.close()
    }

    fun updateDeuda(d: Deuda) {
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_D_TIPO, d.tipo); put(COLUMN_D_CONCEPTO, d.concepto)
            put(COLUMN_D_DESC, d.descripcion); put(COLUMN_D_TOTAL, d.totalDeuda); put(COLUMN_D_PAGADO, d.totalPagado)
        }
        db.update(TABLE_DEUDAS, v, "$COLUMN_D_ID = ?", arrayOf(d.id))
        db.close()
    }

    fun getAllDeudas(): List<Deuda> {
        val lista = mutableListOf<Deuda>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DEUDAS", null)
        while (cursor.moveToNext()) {
            lista.add(Deuda(cursor.getInt(0).toString(), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getDouble(4), cursor.getDouble(5)))
        }
        cursor.close()
        return lista
    }

    fun deleteDeuda(id: String) {
        val db = writableDatabase
        db.delete(TABLE_DEUDAS, "$COLUMN_D_ID = ?", arrayOf(id))
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
