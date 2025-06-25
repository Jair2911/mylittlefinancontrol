package com.example.mylittlefinancontrol

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlin.toString

class NotasDatabaseHelper (context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
){
    private val TAG = "NotasHelpers"

    companion object{
        private const val DATABASE_NAME = "finanzas.db"

        private const val DATABASE_VERSION = 5
        private const val TABLE_MOVIMIENTO = "movimiento"
        private const val COLUMN_MOVIMIENTO_ID = "id"
        private const val COLUMN_MOVIMIENTO_VALOR = "valor"
        private const val COLUMN_MOVIMIENTO_DESCRIPTION = "descripcion"
        private const val COLUMN_MOVIMIENTO_TIPO_REG = "tipo_reg"
        private const val COLUMN_MOVIMIENTO_FECHA = "fecha"

        private const val TABLE_BALANCE = "balance"
        private const val COLUMN_BALANCE_ID = "id"
        private const val COLUMN_BALANCE_VALOR = "valor"
        private const val COLUMN_BALANCE_FECHA = "fecha"

        private const val TABLE_DEUDA = "deuda"
        private const val COLUMN_DEUDA_ID = "id"
        private const val COLUMN_DEUDA_VALOR = "valor"
        private const val COLUMN_DEUDA_FECHA = "fecha"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // SQL para crear la tabla NOTES
        val createNotesTableQuery = """
            CREATE TABLE $TABLE_MOVIMIENTO (
                $COLUMN_MOVIMIENTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MOVIMIENTO_DESCRIPTION TEXT,
                $COLUMN_MOVIMIENTO_VALOR INTEGER,
                $COLUMN_MOVIMIENTO_TIPO_REG TEXT,
                $COLUMN_MOVIMIENTO_FECHA TEXT
            )
        """.trimIndent()
        db?.execSQL(createNotesTableQuery)

        // **NUEVO: SQL para crear la tabla BALANCE**
        val createBalanceTableQuery = """
            CREATE TABLE $TABLE_BALANCE (
                $COLUMN_BALANCE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BALANCE_VALOR REAL, -- Usamos REAL para valores decimales
                $COLUMN_BALANCE_FECHA TEXT
            )
        """.trimIndent()
        db?.execSQL(createBalanceTableQuery)

        // **NUEVO: SQL para crear la tabla DEUDA**
        val createDeudaTableQuery = """
            CREATE TABLE $TABLE_DEUDA (
                $COLUMN_DEUDA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DEUDA_VALOR REAL, -- Usamos REAL para valores decimales
                $COLUMN_DEUDA_FECHA TEXT
            )
        """.trimIndent()
        db?.execSQL(createDeudaTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_MOVIMIENTO"
        val dropBalanceTableQuery = "DROP TABLE IF EXISTS $TABLE_BALANCE"
        val dropDeudaTableQuery = "DROP TABLE IF EXISTS $TABLE_DEUDA"

        db?.execSQL(dropTableQuery)
        db?.execSQL(dropBalanceTableQuery)
        db?.execSQL(dropDeudaTableQuery)

        onCreate(db)
    }

    fun insertNota(movimiento: Movimiento){
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_MOVIMIENTO_DESCRIPTION, movimiento.descripcion)
            put(COLUMN_MOVIMIENTO_VALOR, movimiento.valor)
            put(COLUMN_MOVIMIENTO_TIPO_REG, movimiento.tipo_reg)
            put(COLUMN_MOVIMIENTO_FECHA, movimiento.fecha)
        }

        db.insert(TABLE_MOVIMIENTO, null, values)
        db.close()
    }

    fun getAllNotas() : List<Movimiento>{
        val listaNotas = mutableListOf<Movimiento>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_MOVIMIENTO ORDER BY $COLUMN_MOVIMIENTO_ID DESC"
        val cursor = db.rawQuery(query, null)

        Log.d(TAG, "${cursor}")
        while(cursor.moveToNext()){
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_ID))
            val descripcion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_DESCRIPTION))
            val valor = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_VALOR))
            val tipo_reg = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_TIPO_REG))
            val fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_FECHA))

            val movimiento = Movimiento(id, descripcion, valor, tipo_reg, fecha)
            listaNotas.add(movimiento)
        }
        cursor.close()
        db.close()

        return listaNotas
    }

    fun getIdMovimiento(idNota : Int) : Movimiento {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_MOVIMIENTO WHERE $COLUMN_MOVIMIENTO_ID = $idNota"
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()

        val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_ID))
        val descripcion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_DESCRIPTION))
        val valor = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_VALOR))
        // CORRECCIÓN AQUÍ: Asegúrate de usar la columna correcta para tipo_reg y fecha
        val tipo_reg = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_TIPO_REG)) // Antes estaba mal
        val fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_FECHA))       // Antes estaba mal

        cursor.close()
        db.close()

        return Movimiento(id,descripcion,valor.toInt(),tipo_reg,fecha)
    }

    fun updateMovimiento(movimiento : Movimiento){
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MOVIMIENTO_DESCRIPTION, movimiento.descripcion)
            put(COLUMN_MOVIMIENTO_VALOR, movimiento.valor)
            put(COLUMN_MOVIMIENTO_TIPO_REG, movimiento.tipo_reg)
            put(COLUMN_MOVIMIENTO_FECHA, movimiento.fecha)
        }

        val whereClause = "$COLUMN_MOVIMIENTO_ID = ?"
        val whereArgs = arrayOf(movimiento.id.toString())
        db.update(TABLE_MOVIMIENTO, values, whereClause, whereArgs)
        db.close()
    }

    fun deleteMovimiento(id: Int): Int {
        val db = writableDatabase
        val selection = "$COLUMN_MOVIMIENTO_ID = ?"
        val selectionArgs = arrayOf(id.toString())

        val deletedRows = db.delete(
            TABLE_MOVIMIENTO,
            selection,
            selectionArgs
        )
        db.close()

        return deletedRows
    }

    fun getDateLastReg(): String{
        val db = readableDatabase
        var lastRegDate = "No hay registro de fecha"
        val query = "SELECT $COLUMN_MOVIMIENTO_FECHA FROM $TABLE_MOVIMIENTO ORDER BY $COLUMN_MOVIMIENTO_FECHA DESC LIMIT 1"

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            lastRegDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_FECHA))
        }

        cursor.close()
        db.close()

        return lastRegDate.toString();
    }

    fun getValue(): Int{
        val db = readableDatabase
        var lastRegDate = 0
        val query = "SELECT $COLUMN_MOVIMIENTO_VALOR FROM $TABLE_MOVIMIENTO ORDER BY $COLUMN_MOVIMIENTO_FECHA DESC LIMIT 1"

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            lastRegDate = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_VALOR))
        }

        cursor.close()
        db.close()

        return lastRegDate
    }

    fun getDescripcion(): String{
        val db = readableDatabase
        var lastRegDate = "No hay registro de movimiento"
        val query = "SELECT $COLUMN_MOVIMIENTO_DESCRIPTION FROM $TABLE_MOVIMIENTO ORDER BY $COLUMN_MOVIMIENTO_FECHA DESC LIMIT 1"

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            lastRegDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOVIMIENTO_DESCRIPTION))
        }

        cursor.close()
        db.close()

        return lastRegDate.toString()
    }

    fun getValueAll(): Int{
        val db = readableDatabase
        var totalSum: Double = 0.0 // Inicializamos la suma a 0.0

        val query = "SELECT SUM($COLUMN_MOVIMIENTO_VALOR) FROM $TABLE_MOVIMIENTO"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            totalSum = cursor.getDouble(0)
        }

        cursor.close()
        db.close()

        return totalSum.toInt() // Por ejemplo, "1234.50"
    }

//    fun insertBalance(valor: Double, fecha: String) {
//        val db = writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_BALANCE_VALOR, valor)
//            put(COLUMN_BALANCE_FECHA, fecha)
//        }
//        db.insert(TABLE_BALANCE, null, values)
//        db.close()
//    }
//
//    fun getLastBalance(): Double? {
//        val db = readableDatabase
//        var lastBalance: Double? = null
//        // Ordenar por ID descendente para el último registro insertado
//        val query = "SELECT $COLUMN_BALANCE_VALOR FROM $TABLE_BALANCE ORDER BY $COLUMN_BALANCE_ID DESC LIMIT 1"
//        val cursor = db.rawQuery(query, null)
//
//        if (cursor.moveToFirst()) {
//            lastBalance = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BALANCE_VALOR))
//        }
//        cursor.close()
//        db.close()
//        return lastBalance
//    }
//
//    // **NUEVO: Funciones CRUD para Deuda**
//    fun insertDeuda(valor: Double, fecha: String) {
//        val db = writableDatabase
//        val values = ContentValues().apply {
//            put(COLUMN_DEUDA_VALOR, valor)
//            put(COLUMN_DEUDA_FECHA, fecha)
//        }
//        db.insert(TABLE_DEUDA, null, values)
//        db.close()
//    }
//
//    fun getLastDeuda(): Double? {
//        val db = readableDatabase
//        var lastDeuda: Double? = null
//        // Ordenar por ID descendente para el último registro insertado
//        val query = "SELECT $COLUMN_DEUDA_VALOR FROM $TABLE_DEUDA ORDER BY $COLUMN_DEUDA_ID DESC LIMIT 1"
//        val cursor = db.rawQuery(query, null)
//
//        if (cursor.moveToFirst()) {
//            lastDeuda = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DEUDA_VALOR))
//        }
//        cursor.close()
//        db.close()
//        return lastDeuda
//    }
}
