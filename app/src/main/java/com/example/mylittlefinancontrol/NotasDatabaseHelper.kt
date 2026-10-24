package com.example.mylittlefinancontrol

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.mylittlefinancontrol.Modelos.Ahorro
import com.example.mylittlefinancontrol.Modelos.CategoriaGastoItem
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.Modelos.ResumenPeriodoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ResumenFinancieroOptimizado(
    val totalIngresos: Double,
    val totalGastos: Double,
    val balanceTotal: Double
)

data class DashboardCache(
    val balanceTotal: Double,
    val ingresosTotales: Double,
    val gastosTotales: Double,
    val ingresosMes: Double,
    val gastosMes: Double,
    val totalGastosFijos: Double,
    val totalDeudasRestante: Double,
    val countGastosMes: Int,
    val countGF: Int,
    val countDeudas: Int,
    val periodosResumen: Map<String, Pair<Double, String>>
)

class NotasDatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "finanzas.db"
        private const val DATABASE_VERSION = 14

        private const val TABLE_MOVIMIENTO = "movimiento"
        private const val COLUMN_MOV_ID = "id"
        private const val COLUMN_MOV_DESC = "descripcion"
        private const val COLUMN_MOV_VALOR = "valor"
        private const val COLUMN_MOV_TIPO = "tipo_reg"
        private const val COLUMN_MOV_FECHA = "fecha"
        private const val COLUMN_MOV_GF_ID = "gasto_fijo_id"
        private const val COLUMN_MOV_Q_PAGADA = "quincena_pagada"
        private const val COLUMN_MOV_CATEGORIA = "categoria"
        private const val COLUMN_MOV_FECHA_CREACION = "fecha_creacion"
        private const val COLUMN_MOV_METODO_PAGO = "metodo_pago"
        private const val COLUMN_MOV_DEUDA_ID = "deuda_id"
        private const val COLUMN_MOV_AHORRO_ID = "ahorro_id"

        private const val TABLE_CONTROL_ANUAL = "control_anual_gastos_fijos"
        private const val COLUMN_CA_GF_ID = "gasto_fijo_id"
        private const val COLUMN_CA_ANIO_MES = "anio_mes"
        private const val COLUMN_CA_QUINCENA = "quincena"
        private const val COLUMN_CA_PAGADO_MANUAL = "pagado_manual"

        private const val TABLE_METODOS_PAGO = "metodos_pago"
        private const val COLUMN_MP_ID = "id"
        private const val COLUMN_MP_NOMBRE = "nombre"
        private const val COLUMN_MP_TIPO = "tipo"
        private const val COLUMN_MP_ULTIMOS4 = "ultimos4"
        private const val COLUMN_MP_PREDETERMINADO = "es_predeterminado"

        private const val TABLE_GASTOS_FIJOS = "gastos_fijos"
        private const val COLUMN_GF_ID = "id"
        private const val COLUMN_GF_CONCEPTO = "concepto"
        private const val COLUMN_GF_CATEGORIA = "categoria"
        private const val COLUMN_GF_MONTO = "monto"
        private const val COLUMN_GF_DIA = "diaPago"
        private const val COLUMN_GF_PERIODO = "periodo"
        private const val COLUMN_GF_U_PAGO_Q1 = "ultimo_pago_q1"
        private const val COLUMN_GF_U_PAGO_Q2 = "ultimo_pago_q2"
        private const val COLUMN_GF_RECORDATORIO = "recordatorio"

        private const val TABLE_DEUDAS = "deudas"
        private const val COLUMN_D_ID = "id"
        private const val COLUMN_D_TIPO = "tipo"
        private const val COLUMN_D_CONCEPTO = "concepto"
        private const val COLUMN_D_DESC = "descripcion"
        private const val COLUMN_D_TOTAL = "totalDeuda"
        private const val COLUMN_D_PAGADO = "totalPagado"
        private const val COLUMN_D_FECHA_INI = "fecha_inicio"
        private const val COLUMN_D_FECHA_FIN = "fecha_fin"
        private const val COLUMN_D_TASA_INTERES = "tasa_interes"
        private const val COLUMN_D_TIPO_INTERES = "tipo_interes"
        private const val COLUMN_D_CUOTAS = "cuotas"
        private const val COLUMN_D_VALOR_CUOTA = "valor_cuota"

        private const val TABLE_AHORROS = "ahorros"
        private const val COLUMN_A_ID = "id"
        private const val COLUMN_A_TITULO = "titulo"
        private const val COLUMN_A_CATEGORIA = "categoria"
        private const val COLUMN_A_MONTO_ACTUAL = "monto_actual"
        private const val COLUMN_A_MONTO_OBJETIVO = "monto_objetivo"
        private const val COLUMN_A_FECHA_META = "fecha_meta"
        private const val COLUMN_A_ICONO = "icono"
        private const val COLUMN_A_DESC = "descripcion"
        private const val COLUMN_A_UBICACION = "ubicacion"

        private const val TABLE_CATEGORIAS = "categorias"
        private const val COLUMN_CAT_ID = "id"
        private const val COLUMN_CAT_NOMBRE = "nombre"
        private const val COLUMN_CAT_MODULO = "modulo"
        private const val COLUMN_CAT_ICONO = "icono"

        @Volatile
        private var INSTANCE: NotasDatabaseHelper? = null

        @JvmStatic
        fun getInstance(context: Context): NotasDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotasDatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun obtenerTimestampParaOrdenamiento(mov: Movimiento): Long {
            val fechaCreacionStr = mov.fechaCreacion.trim()
            if (fechaCreacionStr.isNotBlank()) {
                val formatosCreacion = arrayOf(
                    "yyyy-MM-dd HH:mm:ss",
                    "dd/MM/yyyy hh:mm a",
                    "dd/MM/yyyy HH:mm:ss",
                    "yyyy-MM-dd"
                )
                for (fmt in formatosCreacion) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.US)
                        val date = sdf.parse(fechaCreacionStr)
                        if (date != null) return date.time
                    } catch (_: Exception) {}
                }
            }

            val fechaStr = mov.fecha.trim()
            if (fechaStr.isNotBlank()) {
                val formatos = arrayOf(
                    "dd/MM/yyyy hh:mm a",
                    "dd/MM/yyyy HH:mm:ss",
                    "dd/MM/yyyy",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd"
                )
                for (fmt in formatos) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.US)
                        val date = sdf.parse(fechaStr)
                        if (date != null) return date.time
                    } catch (_: Exception) {}
                }
            }

            return 0L
        }
    }

    // --- SISTEMA DE CACHÉ EN MEMORIA RAM ---
    @Volatile
    private var cachedDashboard: DashboardCache? = null

    @Volatile
    private var cachedNotas: List<Movimiento>? = null

    @Volatile
    private var cachedGastosFijos: List<GastoFijo>? = null

    @Volatile
    private var cachedDeudas: List<Deuda>? = null

    @Volatile
    private var cachedAhorros: List<Ahorro>? = null

    @Volatile
    private var cachedCategorias: MutableMap<String, List<String>> = mutableMapOf()

    fun invalidateCache() {
        cachedDashboard = null
        cachedNotas = null
        cachedGastosFijos = null
        cachedDeudas = null
        cachedAhorros = null
        cachedCategorias.clear()
        invalidateMetodosPagoCache()
    }

    fun getCachedDashboardImmediate(): DashboardCache? = cachedDashboard
    fun getCachedNotasImmediate(): List<Movimiento>? = cachedNotas
    fun getCachedGastosFijosImmediate(): List<GastoFijo>? = cachedGastosFijos
    fun getCachedDeudasImmediate(): List<Deuda>? = cachedDeudas
    fun getCachedAhorrosImmediate(): List<Ahorro>? = cachedAhorros

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        try {
            db.enableWriteAheadLogging()
        } catch (e: Exception) {
            Log.e("NotasDatabaseHelper", "Error al activar WAL mode", e)
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE $TABLE_MOVIMIENTO ($COLUMN_MOV_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_MOV_DESC TEXT, $COLUMN_MOV_VALOR INTEGER, $COLUMN_MOV_TIPO TEXT, $COLUMN_MOV_FECHA TEXT, $COLUMN_MOV_GF_ID INTEGER, $COLUMN_MOV_Q_PAGADA INTEGER, $COLUMN_MOV_CATEGORIA TEXT DEFAULT 'Varios', $COLUMN_MOV_FECHA_CREACION TEXT, $COLUMN_MOV_METODO_PAGO TEXT DEFAULT 'Efectivo', $COLUMN_MOV_DEUDA_ID TEXT, $COLUMN_MOV_AHORRO_ID TEXT)")
        db?.execSQL("CREATE TABLE $TABLE_GASTOS_FIJOS ($COLUMN_GF_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_GF_CONCEPTO TEXT, $COLUMN_GF_CATEGORIA TEXT, $COLUMN_GF_MONTO REAL, $COLUMN_GF_DIA TEXT, $COLUMN_GF_PERIODO TEXT, $COLUMN_GF_U_PAGO_Q1 TEXT, $COLUMN_GF_U_PAGO_Q2 TEXT, $COLUMN_GF_RECORDATORIO INTEGER DEFAULT 1)")
        db?.execSQL("CREATE TABLE $TABLE_DEUDAS ($COLUMN_D_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_D_TIPO TEXT, $COLUMN_D_CONCEPTO TEXT, $COLUMN_D_DESC TEXT, $COLUMN_D_TOTAL REAL, $COLUMN_D_PAGADO REAL, $COLUMN_D_FECHA_INI TEXT, $COLUMN_D_FECHA_FIN TEXT, $COLUMN_D_TASA_INTERES REAL DEFAULT 0, $COLUMN_D_TIPO_INTERES TEXT DEFAULT 'Anual', $COLUMN_D_CUOTAS INTEGER DEFAULT 12, $COLUMN_D_VALOR_CUOTA REAL DEFAULT 0)")
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_AHORROS ($COLUMN_A_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_A_TITULO TEXT, $COLUMN_A_CATEGORIA TEXT, $COLUMN_A_MONTO_ACTUAL REAL, $COLUMN_A_MONTO_OBJETIVO REAL, $COLUMN_A_FECHA_META TEXT, $COLUMN_A_ICONO TEXT, $COLUMN_A_DESC TEXT, $COLUMN_A_UBICACION TEXT DEFAULT 'Cuenta / Bolsillo')")
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIAS ($COLUMN_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_CAT_NOMBRE TEXT NOT NULL, $COLUMN_CAT_MODULO TEXT NOT NULL, $COLUMN_CAT_ICONO TEXT DEFAULT 'category', UNIQUE($COLUMN_CAT_NOMBRE, $COLUMN_CAT_MODULO) ON CONFLICT IGNORE)")
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_METODOS_PAGO ($COLUMN_MP_ID TEXT PRIMARY KEY, $COLUMN_MP_NOMBRE TEXT NOT NULL, $COLUMN_MP_TIPO TEXT NOT NULL, $COLUMN_MP_ULTIMOS4 TEXT, $COLUMN_MP_PREDETERMINADO INTEGER DEFAULT 0)")
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CONTROL_ANUAL ($COLUMN_CA_GF_ID TEXT, $COLUMN_CA_ANIO_MES TEXT, $COLUMN_CA_QUINCENA INTEGER, $COLUMN_CA_PAGADO_MANUAL INTEGER, PRIMARY KEY($COLUMN_CA_GF_ID, $COLUMN_CA_ANIO_MES, $COLUMN_CA_QUINCENA))")

        insertarCategoriasDefecto(db)
        insertarMetodoPagoPorDefecto(db)
        crearIndices(db)
    }

    private fun insertarMetodoPagoPorDefecto(db: SQLiteDatabase?) {
        db?.execSQL("INSERT OR IGNORE INTO $TABLE_METODOS_PAGO ($COLUMN_MP_ID, $COLUMN_MP_NOMBRE, $COLUMN_MP_TIPO, $COLUMN_MP_ULTIMOS4, $COLUMN_MP_PREDETERMINADO) VALUES ('efectivo_default', 'Efectivo', 'EFECTIVO', '', 1)")
    }

    private fun crearIndices(db: SQLiteDatabase?) {
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_mov_fecha ON $TABLE_MOVIMIENTO($COLUMN_MOV_FECHA)")
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_mov_fecha_creacion ON $TABLE_MOVIMIENTO($COLUMN_MOV_FECHA_CREACION)")
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_mov_tipo ON $TABLE_MOVIMIENTO($COLUMN_MOV_TIPO)")
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_mov_categoria ON $TABLE_MOVIMIENTO($COLUMN_MOV_CATEGORIA)")
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_mov_gf_id ON $TABLE_MOVIMIENTO($COLUMN_MOV_GF_ID)")
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_gf_dia ON $TABLE_GASTOS_FIJOS($COLUMN_GF_DIA)")
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_cat_modulo ON $TABLE_CATEGORIAS($COLUMN_CAT_MODULO)")
    }

    private fun insertarCategoriasDefecto(db: SQLiteDatabase?) {
        val categoriasMovimiento = listOf(
            "Vivienda", "Servicios", "Comida", "Supermercado", "Tienda",
            "Ocio", "Hobbies", "Ahorro", "Transporte", "Cine",
            "Préstamo", "Gasto Fijo", "Pago Deuda"
        )
        for (cat in categoriasMovimiento) {
            db?.execSQL("INSERT OR IGNORE INTO $TABLE_CATEGORIAS ($COLUMN_CAT_NOMBRE, $COLUMN_CAT_MODULO) VALUES (?, 'MOVIMIENTO')", arrayOf(cat))
        }

        val categoriasGastoFijo = listOf("Vivienda", "Servicios", "Telecom", "Educación", "Streaming", "Otros")
        for (cat in categoriasGastoFijo) {
            db?.execSQL("INSERT OR IGNORE INTO $TABLE_CATEGORIAS ($COLUMN_CAT_NOMBRE, $COLUMN_CAT_MODULO) VALUES (?, 'GASTO_FIJO')", arrayOf(cat))
        }

        val categoriasDeuda = listOf("Tarjeta de Crédito", "Préstamo Bancario", "Préstamo Personal", "Crédito Automotriz", "Préstamo")
        for (cat in categoriasDeuda) {
            db?.execSQL("INSERT OR IGNORE INTO $TABLE_CATEGORIAS ($COLUMN_CAT_NOMBRE, $COLUMN_CAT_MODULO) VALUES (?, 'DEUDA')", arrayOf(cat))
        }
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
        if (oldVersion < 9) {
            db?.execSQL("ALTER TABLE $TABLE_MOVIMIENTO ADD COLUMN $COLUMN_MOV_CATEGORIA TEXT DEFAULT 'Varios'")
            db?.execSQL("ALTER TABLE $TABLE_MOVIMIENTO ADD COLUMN $COLUMN_MOV_FECHA_CREACION TEXT")
        }
        if (oldVersion < 10) {
            db?.execSQL("ALTER TABLE $TABLE_GASTOS_FIJOS ADD COLUMN $COLUMN_GF_RECORDATORIO INTEGER DEFAULT 1")
        }
        if (oldVersion < 11) {
            db?.execSQL("ALTER TABLE $TABLE_DEUDAS ADD COLUMN $COLUMN_D_TASA_INTERES REAL DEFAULT 0")
            db?.execSQL("ALTER TABLE $TABLE_DEUDAS ADD COLUMN $COLUMN_D_TIPO_INTERES TEXT DEFAULT 'Anual'")
            db?.execSQL("ALTER TABLE $TABLE_DEUDAS ADD COLUMN $COLUMN_D_CUOTAS INTEGER DEFAULT 12")
            db?.execSQL("ALTER TABLE $TABLE_DEUDAS ADD COLUMN $COLUMN_D_VALOR_CUOTA REAL DEFAULT 0")
        }
        if (oldVersion < 12) {
            db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIAS ($COLUMN_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_CAT_NOMBRE TEXT NOT NULL, $COLUMN_CAT_MODULO TEXT NOT NULL, $COLUMN_CAT_ICONO TEXT DEFAULT 'category', UNIQUE($COLUMN_CAT_NOMBRE, $COLUMN_CAT_MODULO) ON CONFLICT IGNORE)")
            insertarCategoriasDefecto(db)
        }
        if (oldVersion < 13) {
            db?.execSQL("ALTER TABLE $TABLE_MOVIMIENTO ADD COLUMN $COLUMN_MOV_METODO_PAGO TEXT DEFAULT 'Efectivo'")
            db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_METODOS_PAGO ($COLUMN_MP_ID TEXT PRIMARY KEY, $COLUMN_MP_NOMBRE TEXT NOT NULL, $COLUMN_MP_TIPO TEXT NOT NULL, $COLUMN_MP_ULTIMOS4 TEXT, $COLUMN_MP_PREDETERMINADO INTEGER DEFAULT 0)")
            insertarMetodoPagoPorDefecto(db)
        }
        if (oldVersion < 14) {
            db?.execSQL("ALTER TABLE $TABLE_AHORROS ADD COLUMN $COLUMN_A_UBICACION TEXT DEFAULT 'Cuenta / Bolsillo'")
            db?.execSQL("ALTER TABLE $TABLE_MOVIMIENTO ADD COLUMN $COLUMN_MOV_DEUDA_ID TEXT DEFAULT NULL")
            db?.execSQL("ALTER TABLE $TABLE_MOVIMIENTO ADD COLUMN $COLUMN_MOV_AHORRO_ID TEXT DEFAULT NULL")
            db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CONTROL_ANUAL ($COLUMN_CA_GF_ID TEXT, $COLUMN_CA_ANIO_MES TEXT, $COLUMN_CA_QUINCENA INTEGER, $COLUMN_CA_PAGADO_MANUAL INTEGER, PRIMARY KEY($COLUMN_CA_GF_ID, $COLUMN_CA_ANIO_MES, $COLUMN_CA_QUINCENA))")
        }

        crearIndices(db)
    }

    // --- MÉTODOS PARA MÉTODOS DE PAGO ---
    @Volatile
    private var cachedMetodosPago: List<com.example.mylittlefinancontrol.Modelos.MetodoPago>? = null

    fun invalidateMetodosPagoCache() {
        cachedMetodosPago = null
    }

    fun obtenerMetodosPago(): List<com.example.mylittlefinancontrol.Modelos.MetodoPago> {
        cachedMetodosPago?.let { return it }
        val lista = mutableListOf<com.example.mylittlefinancontrol.Modelos.MetodoPago>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_METODOS_PAGO ORDER BY $COLUMN_MP_PREDETERMINADO DESC, $COLUMN_MP_NOMBRE ASC", null)
        if (cursor.moveToFirst()) {
            do {
                val mp = com.example.mylittlefinancontrol.Modelos.MetodoPago(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MP_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MP_NOMBRE)),
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MP_TIPO)),
                    ultimos4 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MP_ULTIMOS4)),
                    esPredeterminado = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MP_PREDETERMINADO)) == 1
                )
                lista.add(mp)
            } while (cursor.moveToNext())
        }
        cursor.close()
        // Fallback safety
        if (lista.isEmpty()) {
            lista.add(com.example.mylittlefinancontrol.Modelos.MetodoPago("efectivo_default", "Efectivo", "EFECTIVO", "", true))
        }
        cachedMetodosPago = lista
        return lista
    }

    fun insertarOActualizarMetodoPago(mp: com.example.mylittlefinancontrol.Modelos.MetodoPago): Boolean {
        val db = writableDatabase
        if (mp.esPredeterminado) {
            db.execSQL("UPDATE $TABLE_METODOS_PAGO SET $COLUMN_MP_PREDETERMINADO = 0")
        }
        val values = ContentValues().apply {
            put(COLUMN_MP_ID, mp.id)
            put(COLUMN_MP_NOMBRE, mp.nombre)
            put(COLUMN_MP_TIPO, mp.tipo)
            put(COLUMN_MP_ULTIMOS4, mp.ultimos4)
            put(COLUMN_MP_PREDETERMINADO, if (mp.esPredeterminado) 1 else 0)
        }
        val result = db.insertWithOnConflict(TABLE_METODOS_PAGO, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        invalidateMetodosPagoCache()
        return result != -1L
    }

    fun eliminarMetodoPago(id: String): Boolean {
        if (id == "efectivo_default") return false // Prevent deleting default
        val db = writableDatabase
        val result = db.delete(TABLE_METODOS_PAGO, "$COLUMN_MP_ID = ?", arrayOf(id))
        invalidateMetodosPagoCache()
        return result > 0
    }

    // --- MÉTODOS PARA CATEGORÍAS DINÁMICAS ---
    fun obtenerCategoriasPorModulo(modulo: String): List<String> {
        if (cachedCategorias.containsKey(modulo)) {
            return cachedCategorias[modulo]!!
        }

        val lista = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_CAT_NOMBRE FROM $TABLE_CATEGORIAS WHERE $COLUMN_CAT_MODULO = ? ORDER BY $COLUMN_CAT_NOMBRE ASC",
            arrayOf(modulo)
        )

        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(0)
                if (nombre.isNotBlank() && nombre != "Otra...") {
                    lista.add(nombre)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        lista.add("Otra...")
        cachedCategorias[modulo] = lista
        return lista
    }

    fun insertarCategoria(nombre: String, modulo: String): Boolean {
        val nombreLimpio = nombre.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        if (nombreLimpio.isBlank() || nombreLimpio.equals("Otra...", ignoreCase = true)) return false

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CAT_NOMBRE, nombreLimpio)
            put(COLUMN_CAT_MODULO, modulo)
            put(COLUMN_CAT_ICONO, "category")
        }

        val id = db.insertWithOnConflict(TABLE_CATEGORIAS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        cachedCategorias.remove(modulo) // Invalidar caché de este módulo
        return id != -1L
    }

    fun performWalCheckpoint() {
        try {
            val db = writableDatabase
            val cursor = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)
            cursor.moveToFirst()
            cursor.close()
            Log.d("NotasDatabaseHelper", "WAL Checkpoint completado")
        } catch (e: Exception) {
            Log.e("NotasDatabaseHelper", "Error ejecutando WAL Checkpoint", e)
        }
    }

    private fun cursorToMovimiento(cursor: Cursor): Movimiento {
        val idIdx = cursor.getColumnIndex(COLUMN_MOV_ID)
        val descIdx = cursor.getColumnIndex(COLUMN_MOV_DESC)
        val valIdx = cursor.getColumnIndex(COLUMN_MOV_VALOR)
        val tipoIdx = cursor.getColumnIndex(COLUMN_MOV_TIPO)
        val fechaIdx = cursor.getColumnIndex(COLUMN_MOV_FECHA)
        val gfIdx = cursor.getColumnIndex(COLUMN_MOV_GF_ID)
        val qIdx = cursor.getColumnIndex(COLUMN_MOV_Q_PAGADA)
        val catIdx = cursor.getColumnIndex(COLUMN_MOV_CATEGORIA)
        val fcIdx = cursor.getColumnIndex(COLUMN_MOV_FECHA_CREACION)
        val mpIdx = cursor.getColumnIndex(COLUMN_MOV_METODO_PAGO)

        val id = if (idIdx != -1) cursor.getInt(idIdx) else 0
        val desc = if (descIdx != -1) cursor.getString(descIdx) ?: "" else ""
        val valor = if (valIdx != -1) cursor.getInt(valIdx) else 0
        val tipo = if (tipoIdx != -1) cursor.getString(tipoIdx) ?: "" else ""
        val fecha = if (fechaIdx != -1) cursor.getString(fechaIdx) ?: "" else ""
        val gfId = if (gfIdx != -1 && !cursor.isNull(gfIdx)) cursor.getInt(gfIdx) else null
        val qPag = if (qIdx != -1 && !cursor.isNull(qIdx)) cursor.getInt(qIdx) else null
        val cat = if (catIdx != -1 && !cursor.isNull(catIdx)) cursor.getString(catIdx) ?: "Varios" else "Varios"
        val fc = if (fcIdx != -1 && !cursor.isNull(fcIdx)) cursor.getString(fcIdx) ?: fecha else fecha
        val mp = if (mpIdx != -1 && !cursor.isNull(mpIdx)) cursor.getString(mpIdx) ?: "Efectivo" else "Efectivo"

        return Movimiento(
            id = id,
            descripcion = desc,
            valor = valor,
            tipo_reg = tipo,
            fecha = fecha,
            gastoFijoId = gfId,
            quincenaPagada = qPag,
            categoria = cat,
            fechaCreacion = fc,
            metodoPago = mp
        )
    }

    // --- MÉTODOS MOVIMIENTOS ---
    fun insertNota(mov: Movimiento) {
        invalidateCache()
        val db = writableDatabase
        val fechaCreacionFinal: String = if (mov.fechaCreacion.isNotBlank()) mov.fechaCreacion else Constantes.formatoFecha(Constantes.obtenerTiempoD())
        val v = ContentValues().apply {
            put(COLUMN_MOV_DESC, mov.descripcion)
            put(COLUMN_MOV_VALOR, mov.valor)
            put(COLUMN_MOV_TIPO, mov.tipo_reg)
            put(COLUMN_MOV_FECHA, mov.fecha)
            put(COLUMN_MOV_GF_ID, mov.gastoFijoId)
            put(COLUMN_MOV_Q_PAGADA, mov.quincenaPagada)
            put(COLUMN_MOV_CATEGORIA, mov.categoria)
            put(COLUMN_MOV_FECHA_CREACION, fechaCreacionFinal)
            put(COLUMN_MOV_METODO_PAGO, mov.metodoPago)
        }
        db.insert(TABLE_MOVIMIENTO, null, v)
    }

    fun getAllNotas(): List<Movimiento> {
        cachedNotas?.let { return it }
        val lista = mutableListOf<Movimiento>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MOVIMIENTO ORDER BY $COLUMN_MOV_ID DESC", null)
        while (cursor.moveToNext()) {
            lista.add(cursorToMovimiento(cursor))
        }
        cursor.close()

        val sorted = lista.sortedWith(
            compareByDescending<Movimiento> { obtenerTimestampParaOrdenamiento(it) }
                .thenByDescending { it.id }
        )
        cachedNotas = sorted
        return sorted
    }

    fun getIdMovimiento(id: Int): Movimiento {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MOVIMIENTO WHERE $COLUMN_MOV_ID = ?", arrayOf(id.toString()))
        cursor.moveToFirst()
        val mov = cursorToMovimiento(cursor)
        cursor.close()
        return mov
    }

    fun updateMovimiento(mov: Movimiento) {
        invalidateCache()
        val db = writableDatabase
        val fechaCreacionFinal: String = if (mov.fechaCreacion.isNotBlank()) mov.fechaCreacion else Constantes.formatoFecha(Constantes.obtenerTiempoD())
        val v = ContentValues().apply {
            put(COLUMN_MOV_DESC, mov.descripcion)
            put(COLUMN_MOV_VALOR, mov.valor)
            put(COLUMN_MOV_TIPO, mov.tipo_reg)
            put(COLUMN_MOV_FECHA, mov.fecha)
            put(COLUMN_MOV_GF_ID, mov.gastoFijoId)
            put(COLUMN_MOV_Q_PAGADA, mov.quincenaPagada)
            put(COLUMN_MOV_CATEGORIA, mov.categoria)
            put(COLUMN_MOV_FECHA_CREACION, fechaCreacionFinal)
            put(COLUMN_MOV_METODO_PAGO, mov.metodoPago)
        }
        db.update(TABLE_MOVIMIENTO, v, "$COLUMN_MOV_ID = ?", arrayOf(mov.id.toString()))
    }

    fun deleteMovimiento(id: Int): Int {
        invalidateCache()
        val db = writableDatabase
        return db.delete(TABLE_MOVIMIENTO, "$COLUMN_MOV_ID = ?", arrayOf(id.toString()))
    }

    /**
     * Busca si ya existe un movimiento con el mismo valor y cuya fecha
     * coincida en día y hora/minuto (con una tolerancia de ±5 minutos).
     */
    fun buscarMovimientoDuplicado(valor: Int, fechaHora: String): Movimiento? {
        if (valor <= 0 || fechaHora.isBlank()) return null
        
        android.util.Log.d("DUPLICADO_CHECK", "Buscando valor=$valor, fechaNormalizada=$fechaHora vs DB")

        val targetTs = parsearFechaTimestamp(fechaHora)
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_MOVIMIENTO WHERE $COLUMN_MOV_VALOR = ? ORDER BY $COLUMN_MOV_ID DESC",
            arrayOf(valor.toString())
        )

        try {
            while (cursor.moveToNext()) {
                val mov = cursorToMovimiento(cursor)
                val movTs = obtenerTimestampParaOrdenamiento(mov)

                if (targetTs > 0L && movTs > 0L) {
                    val diffMinutos = Math.abs(movTs - targetTs) / (60 * 1000L)
                    if (diffMinutos <= 5L) {
                        return mov
                    }
                } else {
                    val fecha1 = extraerDiaSolo(mov.fecha)
                    val fecha2 = extraerDiaSolo(fechaHora)
                    if (fecha1.isNotBlank() && fecha1 == fecha2) {
                        return mov
                    }
                }
            }
        } finally {
            cursor.close()
        }

        return null
    }

    private fun parsearFechaTimestamp(fechaStr: String): Long {
        val clean = fechaStr.trim()
        val formatos = arrayOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy hh:mm a",
            "yyyy-MM-dd"
        )
        for (fmt in formatos) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val d = sdf.parse(clean)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    private fun extraerDiaSolo(fechaStr: String): String {
        val clean = fechaStr.trim()
        val m1 = java.util.regex.Pattern.compile("(\\d{4})[-/](\\d{2})[-/](\\d{2})").matcher(clean)
        if (m1.find()) {
            return "${m1.group(1)}-${m1.group(2)}-${m1.group(3)}"
        }
        val m2 = java.util.regex.Pattern.compile("(\\d{2})[-/](\\d{2})[-/](\\d{4})").matcher(clean)
        if (m2.find()) {
            return "${m2.group(3)}-${m2.group(2)}-${m2.group(1)}"
        }
        return ""
    }

    // --- MÉTODOS GASTOS FIJOS ---
    fun insertGastoFijo(gf: GastoFijo) {
        invalidateCache()
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_GF_CONCEPTO, gf.concepto); put(COLUMN_GF_CATEGORIA, gf.categoria)
            put(COLUMN_GF_MONTO, gf.monto); put(COLUMN_GF_DIA, gf.diaPago); put(COLUMN_GF_PERIODO, gf.periodo)
            put(COLUMN_GF_U_PAGO_Q1, gf.ultimoPagoQ1); put(COLUMN_GF_U_PAGO_Q2, gf.ultimoPagoQ2)
            put(COLUMN_GF_RECORDATORIO, if (gf.recordatorio) 1 else 0)
        }
        db.insert(TABLE_GASTOS_FIJOS, null, v)
    }

    fun updateGastoFijo(gf: GastoFijo) {
        invalidateCache()
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_GF_CONCEPTO, gf.concepto); put(COLUMN_GF_CATEGORIA, gf.categoria)
            put(COLUMN_GF_MONTO, gf.monto); put(COLUMN_GF_DIA, gf.diaPago); put(COLUMN_GF_PERIODO, gf.periodo)
            put(COLUMN_GF_U_PAGO_Q1, gf.ultimoPagoQ1); put(COLUMN_GF_U_PAGO_Q2, gf.ultimoPagoQ2)
            put(COLUMN_GF_RECORDATORIO, if (gf.recordatorio) 1 else 0)
        }
        db.update(TABLE_GASTOS_FIJOS, v, "$COLUMN_GF_ID = ?", arrayOf(gf.id))
    }

    fun getAllGastosFijos(): List<GastoFijo> {
        cachedGastosFijos?.let { return it }
        val lista = mutableListOf<GastoFijo>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_GASTOS_FIJOS", null)
        val recIdx = cursor.getColumnIndex(COLUMN_GF_RECORDATORIO)

        while (cursor.moveToNext()) {
            val recordatorio = if (recIdx != -1 && !cursor.isNull(recIdx)) cursor.getInt(recIdx) == 1 else true
            lista.add(GastoFijo(
                cursor.getInt(0).toString(), 
                cursor.getString(1), 
                cursor.getString(2), 
                cursor.getDouble(3), 
                cursor.getString(4), 
                cursor.getString(5),
                cursor.getString(6) ?: "",
                cursor.getString(7) ?: "",
                recordatorio
            ))
        }
        cursor.close()
        cachedGastosFijos = lista
        return lista
    }

    fun deleteGastoFijo(id: String) {
        invalidateCache()
        val db = writableDatabase
        db.delete(TABLE_GASTOS_FIJOS, "$COLUMN_GF_ID = ?", arrayOf(id))
    }

    // --- MÉTODOS DEUDAS ---
    fun insertDeuda(d: Deuda) {
        invalidateCache()
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_D_TIPO, d.tipo); put(COLUMN_D_CONCEPTO, d.concepto)
            put(COLUMN_D_DESC, d.descripcion); put(COLUMN_D_TOTAL, d.totalDeuda); put(COLUMN_D_PAGADO, d.totalPagado)
            put(COLUMN_D_FECHA_INI, d.fechaInicio); put(COLUMN_D_FECHA_FIN, d.fechaFin)
            put(COLUMN_D_TASA_INTERES, d.tasaInteres)
            put(COLUMN_D_TIPO_INTERES, d.tipoInteres)
            put(COLUMN_D_CUOTAS, d.cuotas)
            put(COLUMN_D_VALOR_CUOTA, d.valorCuota)
        }
        db.insert(TABLE_DEUDAS, null, v)
    }

    fun updateDeuda(d: Deuda) {
        invalidateCache()
        val db = writableDatabase
        val v = ContentValues().apply {
            put(COLUMN_D_TIPO, d.tipo); put(COLUMN_D_CONCEPTO, d.concepto)
            put(COLUMN_D_DESC, d.descripcion); put(COLUMN_D_TOTAL, d.totalDeuda); put(COLUMN_D_PAGADO, d.totalPagado)
            put(COLUMN_D_FECHA_INI, d.fechaInicio); put(COLUMN_D_FECHA_FIN, d.fechaFin)
            put(COLUMN_D_TASA_INTERES, d.tasaInteres)
            put(COLUMN_D_TIPO_INTERES, d.tipoInteres)
            put(COLUMN_D_CUOTAS, d.cuotas)
            put(COLUMN_D_VALOR_CUOTA, d.valorCuota)
        }
        db.update(TABLE_DEUDAS, v, "$COLUMN_D_ID = ?", arrayOf(d.id))
    }

    fun getAllDeudas(): List<Deuda> {
        cachedDeudas?.let { return it }
        val lista = mutableListOf<Deuda>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DEUDAS", null)

        val tasaIdx = cursor.getColumnIndex(COLUMN_D_TASA_INTERES)
        val tipoIntIdx = cursor.getColumnIndex(COLUMN_D_TIPO_INTERES)
        val cuotasIdx = cursor.getColumnIndex(COLUMN_D_CUOTAS)
        val vCuotaIdx = cursor.getColumnIndex(COLUMN_D_VALOR_CUOTA)

        while (cursor.moveToNext()) {
            val tasa = if (tasaIdx != -1 && !cursor.isNull(tasaIdx)) cursor.getDouble(tasaIdx) else 0.0
            val tipoInt = if (tipoIntIdx != -1 && !cursor.isNull(tipoIntIdx)) cursor.getString(tipoIntIdx) ?: "Anual" else "Anual"
            val cuotas = if (cuotasIdx != -1 && !cursor.isNull(cuotasIdx)) cursor.getInt(cuotasIdx) else 12
            val vCuota = if (vCuotaIdx != -1 && !cursor.isNull(vCuotaIdx)) cursor.getDouble(vCuotaIdx) else 0.0

            lista.add(Deuda(
                cursor.getInt(0).toString(), 
                cursor.getString(1), 
                cursor.getString(2), 
                cursor.getString(3), 
                cursor.getDouble(4), 
                cursor.getDouble(5),
                cursor.getString(6) ?: "",
                cursor.getString(7) ?: "",
                tasa,
                tipoInt,
                cuotas,
                vCuota
            ))
        }
        cursor.close()
        cachedDeudas = lista
        return lista
    }

    fun deleteDeuda(id: String) {
        invalidateCache()
        val db = writableDatabase
        db.delete(TABLE_DEUDAS, "$COLUMN_D_ID = ?", arrayOf(id))
    }

    // --- MÉTODOS AHORROS ---
    fun insertAhorro(a: Ahorro) {
        invalidateCache()
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
    }

    fun updateAhorro(a: Ahorro) {
        invalidateCache()
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
    }

    fun getAllAhorros(): List<Ahorro> {
        cachedAhorros?.let { return it }
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
        cachedAhorros = lista
        return lista
    }

    fun deleteAhorro(id: String) {
        invalidateCache()
        val db = writableDatabase
        db.delete(TABLE_AHORROS, "$COLUMN_A_ID = ?", arrayOf(id))
    }

    // --- CONSULTAS SQL AGREGADAS Y OPTIMIZADAS ---
    fun getResumenFinancieroOptimizado(): ResumenFinancieroOptimizado {
        val db = readableDatabase
        var ingresos = 0.0
        var gastos = 0.0
        var ahorrosMovimientos = 0.0
        val query = "SELECT " +
                "SUM(CASE WHEN LOWER($COLUMN_MOV_TIPO) LIKE '%ingreso%' THEN $COLUMN_MOV_VALOR ELSE 0 END), " +
                "SUM(CASE WHEN LOWER($COLUMN_MOV_TIPO) NOT LIKE '%ingreso%' AND LOWER($COLUMN_MOV_TIPO) != 'ahorro' AND LOWER($COLUMN_MOV_CATEGORIA) != 'ahorro' AND $COLUMN_MOV_AHORRO_ID IS NULL THEN $COLUMN_MOV_VALOR ELSE 0 END), " +
                "SUM(CASE WHEN LOWER($COLUMN_MOV_TIPO) = 'ahorro' OR LOWER($COLUMN_MOV_CATEGORIA) = 'ahorro' OR $COLUMN_MOV_AHORRO_ID IS NOT NULL THEN $COLUMN_MOV_VALOR ELSE 0 END) " +
                "FROM $TABLE_MOVIMIENTO"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            ingresos = cursor.getDouble(0)
            gastos = cursor.getDouble(1)
            ahorrosMovimientos = cursor.getDouble(2)
        }
        cursor.close()
        return ResumenFinancieroOptimizado(
            totalIngresos = Constantes.redondearMonto(ingresos),
            totalGastos = Constantes.redondearMonto(gastos + ahorrosMovimientos), // Para mantener compatibilidad si se usa totalGastos en otros lados
            balanceTotal = Constantes.redondearMonto(ingresos - gastos - ahorrosMovimientos)
        )
    }

    fun getResumenFinanciero(): com.example.mylittlefinancontrol.Modelos.ResumenFinanciero {
        val opt = getResumenFinancieroOptimizado()

        val db = readableDatabase
        var totalGF = 0.0
        val cursorGF = db.rawQuery("SELECT SUM($COLUMN_GF_MONTO) FROM $TABLE_GASTOS_FIJOS", null)
        if (cursorGF.moveToFirst()) {
            totalGF = cursorGF.getDouble(0)
        }
        cursorGF.close()

        var totalDeudas = 0.0
        val cursorD = db.rawQuery("SELECT SUM($COLUMN_D_TOTAL - $COLUMN_D_PAGADO) FROM $TABLE_DEUDAS", null)
        if (cursorD.moveToFirst()) {
            totalDeudas = cursorD.getDouble(0)
        }
        cursorD.close()

        var totalAhorros = 0.0
        var ahorroCuenta = 0.0
        var ahorroAlcancia = 0.0
        
        val cursorA = db.rawQuery("SELECT $COLUMN_A_UBICACION, SUM($COLUMN_A_MONTO_ACTUAL) FROM $TABLE_AHORROS GROUP BY $COLUMN_A_UBICACION", null)
        while (cursorA.moveToNext()) {
            val ubicacion = cursorA.getString(0) ?: ""
            val monto = cursorA.getDouble(1)
            totalAhorros += monto
            if (ubicacion.contains("Alcancía", ignoreCase = true) || ubicacion.contains("Efectivo", ignoreCase = true)) {
                ahorroAlcancia += monto
            } else {
                ahorroCuenta += monto
            }
        }
        cursorA.close()

        return com.example.mylittlefinancontrol.Modelos.ResumenFinanciero(
            balanceTotal = opt.balanceTotal, 
            totalGastosFijos = totalGF, 
            totalDeudasPendientes = totalDeudas,
            totalAhorros = totalAhorros,
            ahorroEnCuenta = ahorroCuenta,
            ahorroEnAlcancia = ahorroAlcancia
        )
    }

    fun getResumenPorRangoFechasSQL(startDate: String, endDate: String): ResumenPeriodoData {
        val db = readableDatabase
        var ingresos = 0.0
        var gastos = 0.0

        val querySum = "SELECT " +
                "SUM(CASE WHEN LOWER($COLUMN_MOV_TIPO) LIKE '%ingreso%' THEN $COLUMN_MOV_VALOR ELSE 0 END), " +
                "SUM(CASE WHEN LOWER($COLUMN_MOV_TIPO) NOT LIKE '%ingreso%' THEN $COLUMN_MOV_VALOR ELSE 0 END) " +
                "FROM $TABLE_MOVIMIENTO " +
                "WHERE SUBSTR(COALESCE(NULLIF($COLUMN_MOV_FECHA, ''), $COLUMN_MOV_FECHA_CREACION), 1, 10) BETWEEN ? AND ?"

        val cursor = db.rawQuery(querySum, arrayOf(startDate, endDate))
        if (cursor.moveToFirst()) {
            ingresos = cursor.getDouble(0)
            gastos = cursor.getDouble(1)
        }
        cursor.close()

        val balance = ingresos - gastos
        return ResumenPeriodoData(
            totalIngresos = Constantes.redondearMonto(ingresos),
            totalGastos = Constantes.redondearMonto(gastos),
            balanceNeto = Constantes.redondearMonto(balance),
            listaCategorias = emptyList()
        )
    }

    /**
     * Consulta SQL rápida e indexada para obtener el total de gastos de un rango de fechas.
     */
    fun getTotalGastosPeriodoSQL(startDate: String, endDate: String): Double {
        val db = readableDatabase
        var gastos = 0.0

        val query = "SELECT SUM($COLUMN_MOV_VALOR) FROM $TABLE_MOVIMIENTO " +
                "WHERE LOWER($COLUMN_MOV_TIPO) NOT LIKE '%ingreso%' " +
                "AND SUBSTR(COALESCE(NULLIF($COLUMN_MOV_FECHA, ''), $COLUMN_MOV_FECHA_CREACION), 1, 10) BETWEEN ? AND ?"

        val cursor = db.rawQuery(query, arrayOf(startDate, endDate))
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            gastos = cursor.getDouble(0)
        }
        cursor.close()

        return Constantes.redondearMonto(gastos)
    }

    suspend fun getDashboardDataCached(
        calcularFechasPeriodo: (String) -> Pair<Pair<String, String>, Pair<String, String>>
    ): DashboardCache = withContext(Dispatchers.IO) {
        cachedDashboard?.let { return@withContext it }

        val deferredGF = async { getAllGastosFijos() }
        val deferredDeudas = async { getAllDeudas() }
        val deferredNotas = async { getAllNotas() }
        val deferredResumenFin = async { getResumenFinancieroOptimizado() }

        val periodos = listOf("SEMANAL", "MENSUAL", "SEMESTRAL", "ANUAL")
        val deferredPeriodos = periodos.map { p ->
            async {
                val (rangeAct, rangeAnt) = calcularFechasPeriodo(p)
                val resAct = getResumenPorRangoFechasSQL(rangeAct.first, rangeAct.second)
                val resAnt = getResumenPorRangoFechasSQL(rangeAnt.first, rangeAnt.second)

                val varPct = if (resAnt.totalGastos > 0) {
                    ((resAct.totalGastos - resAnt.totalGastos) / resAnt.totalGastos) * 100.0
                } else 0.0

                val varTxt = if (varPct > 0) "+${varPct.toInt()}% vs ant." else if (varPct < 0) "${varPct.toInt()}% vs ant." else "0% vs ant."
                p to Pair(resAct.balanceNeto, varTxt)
            }
        }

        val listaGF = deferredGF.await()
        val listaDeudas = deferredDeudas.await()
        val todosMovs = deferredNotas.await()
        val resumenFin = deferredResumenFin.await()
        val periodosRes = deferredPeriodos.awaitAll().toMap()

        val cal = Calendar.getInstance()
        val mesActual = cal.get(Calendar.MONTH)
        val anioActual = cal.get(Calendar.YEAR)
        var ingMes = 0.0
        var gastMes = 0.0
        var cantGastosMes = 0

        for (mov in todosMovs) {
            val isIngreso = mov.tipo_reg.contains("INGRESO", true)
            if (!isIngreso) cantGastosMes++

            try {
                val f = if (mov.fecha.isNotBlank()) mov.fecha else mov.fechaCreacion
                val fClean = if (f.length >= 10) f.substring(0, 10) else f.split(" ")[0]
                val partes = fClean.split("-")
                if (partes.size >= 2) {
                    val anio = partes[0].toInt()
                    val mes = partes[1].toInt() - 1
                    if (anio == anioActual && mes == mesActual) {
                        if (isIngreso) ingMes += mov.valor else gastMes += mov.valor
                    }
                }
            } catch (_: Exception) {}
        }

        val newCache = DashboardCache(
            balanceTotal = resumenFin.balanceTotal,
            ingresosTotales = resumenFin.totalIngresos,
            gastosTotales = resumenFin.totalGastos,
            ingresosMes = ingMes,
            gastosMes = gastMes,
            totalGastosFijos = listaGF.sumOf { it.monto },
            totalDeudasRestante = listaDeudas.sumOf { (it.totalDeuda - it.totalPagado).coerceAtLeast(0.0) },
            countGastosMes = cantGastosMes,
            countGF = listaGF.size,
            countDeudas = listaDeudas.size,
            periodosResumen = periodosRes
        )

        cachedDashboard = newCache
        return@withContext newCache
    }

    // --- CONSULTAS DE PERIODOS Y ANALÍTICA ---
    fun getMovimientosPorRangoFechas(startDate: String, endDate: String, todos: List<Movimiento>? = null): List<Movimiento> {
        val lista = todos ?: getAllNotas()
        return lista.filter { mov ->
            val f = if (mov.fecha.isNotBlank()) mov.fecha else mov.fechaCreacion
            val fClean = if (f.length >= 10) f.substring(0, 10) else f.split(" ")[0]
            fClean >= startDate && fClean <= endDate
        }
    }

    fun getResumenPorRangoFechas(startDate: String, endDate: String, todos: List<Movimiento>? = null): ResumenPeriodoData {
        val movs = getMovimientosPorRangoFechas(startDate, endDate, todos)
        
        var ingresos = 0.0
        var gastos = 0.0

        val catMap = mutableMapOf<String, Double>()
        val catCountMap = mutableMapOf<String, Int>()

        for (m in movs) {
            val valor = m.valor.toDouble()
            if (m.tipo_reg.contains("INGRESO", true)) {
                ingresos += valor
            } else {
                gastos += valor
                val cat = if (m.categoria.isNotBlank()) m.categoria else "Varios"
                catMap[cat] = (catMap[cat] ?: 0.0) + valor
                catCountMap[cat] = (catCountMap[cat] ?: 0) + 1
            }
        }

        val listaCat = catMap.map { (nombre, total) ->
            val pct = if (gastos > 0) (total / gastos) * 100.0 else 0.0
            CategoriaGastoItem(
                nombreCategoria = nombre,
                totalMonto = total,
                porcentaje = Constantes.redondearMonto(pct),
                conteoTransacciones = catCountMap[nombre] ?: 0
            )
        }.sortedByDescending { it.totalMonto }

        val balance = ingresos - gastos

        return ResumenPeriodoData(
            totalIngresos = Constantes.redondearMonto(ingresos),
            totalGastos = Constantes.redondearMonto(gastos),
            balanceNeto = Constantes.redondearMonto(balance),
            listaCategorias = listaCat
        )
    }

    fun clearAllData() {
        invalidateCache()
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_MOVIMIENTO, null, null)
            db.delete(TABLE_GASTOS_FIJOS, null, null)
            db.delete(TABLE_DEUDAS, null, null)
            db.delete(TABLE_AHORROS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // --- TRANSACCIONES ACID ---

    fun insertarAbonoDeudaTransaccional(mov: Movimiento, deudaId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Actualizar deuda
            db.execSQL("UPDATE $TABLE_DEUDAS SET $COLUMN_D_PAGADO = $COLUMN_D_PAGADO + ? WHERE $COLUMN_D_ID = ?", arrayOf(mov.valor.toDouble(), deudaId))
            // Insertar movimiento
            val v = ContentValues().apply {
                put(COLUMN_MOV_DESC, mov.descripcion)
                put(COLUMN_MOV_VALOR, mov.valor)
                put(COLUMN_MOV_TIPO, mov.tipo_reg)
                put(COLUMN_MOV_FECHA, mov.fecha)
                put(COLUMN_MOV_GF_ID, mov.gastoFijoId)
                put(COLUMN_MOV_Q_PAGADA, mov.quincenaPagada)
                put(COLUMN_MOV_CATEGORIA, mov.categoria)
                put(COLUMN_MOV_FECHA_CREACION, if (mov.fechaCreacion.isNotBlank()) mov.fechaCreacion else Constantes.formatoFecha(Constantes.obtenerTiempoD()))
                put(COLUMN_MOV_METODO_PAGO, mov.metodoPago)
                put(COLUMN_MOV_DEUDA_ID, deudaId)
            }
            db.insert(TABLE_MOVIMIENTO, null, v)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            invalidateCache()
        }
    }

    fun actualizarAbonoDeudaTransaccional(movViejo: Movimiento, movNuevo: Movimiento, deudaId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val diferencia = movNuevo.valor - movViejo.valor
            if (diferencia != 0) {
                db.execSQL("UPDATE $TABLE_DEUDAS SET $COLUMN_D_PAGADO = $COLUMN_D_PAGADO + ? WHERE $COLUMN_D_ID = ?", arrayOf(diferencia.toDouble(), deudaId))
            }
            
            val v = ContentValues().apply {
                put(COLUMN_MOV_DESC, movNuevo.descripcion)
                put(COLUMN_MOV_VALOR, movNuevo.valor)
                put(COLUMN_MOV_TIPO, movNuevo.tipo_reg)
                put(COLUMN_MOV_FECHA, movNuevo.fecha)
                put(COLUMN_MOV_GF_ID, movNuevo.gastoFijoId)
                put(COLUMN_MOV_Q_PAGADA, movNuevo.quincenaPagada)
                put(COLUMN_MOV_CATEGORIA, movNuevo.categoria)
                put(COLUMN_MOV_FECHA_CREACION, if (movNuevo.fechaCreacion.isNotBlank()) movNuevo.fechaCreacion else Constantes.formatoFecha(Constantes.obtenerTiempoD()))
                put(COLUMN_MOV_METODO_PAGO, movNuevo.metodoPago)
                put(COLUMN_MOV_DEUDA_ID, deudaId)
            }
            db.update(TABLE_MOVIMIENTO, v, "$COLUMN_MOV_ID = ?", arrayOf(movNuevo.id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            invalidateCache()
        }
    }

    fun insertarAbonoAhorroTransaccional(mov: Movimiento, ahorroId: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("UPDATE $TABLE_AHORROS SET $COLUMN_A_MONTO_ACTUAL = $COLUMN_A_MONTO_ACTUAL + ? WHERE $COLUMN_A_ID = ?", arrayOf(mov.valor.toDouble(), ahorroId))
            
            val v = ContentValues().apply {
                put(COLUMN_MOV_DESC, mov.descripcion)
                put(COLUMN_MOV_VALOR, mov.valor)
                put(COLUMN_MOV_TIPO, mov.tipo_reg)
                put(COLUMN_MOV_FECHA, mov.fecha)
                put(COLUMN_MOV_GF_ID, mov.gastoFijoId)
                put(COLUMN_MOV_Q_PAGADA, mov.quincenaPagada)
                put(COLUMN_MOV_CATEGORIA, mov.categoria)
                put(COLUMN_MOV_FECHA_CREACION, if (mov.fechaCreacion.isNotBlank()) mov.fechaCreacion else Constantes.formatoFecha(Constantes.obtenerTiempoD()))
                put(COLUMN_MOV_METODO_PAGO, mov.metodoPago)
                put(COLUMN_MOV_AHORRO_ID, ahorroId.toString())
            }
            db.insert(TABLE_MOVIMIENTO, null, v)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            invalidateCache()
        }
    }

    fun eliminarMovimientoTransaccional(mov: Movimiento) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Revertir efectos contables según el origen del movimiento
            val cursor = db.rawQuery("SELECT $COLUMN_MOV_DEUDA_ID, $COLUMN_MOV_AHORRO_ID FROM $TABLE_MOVIMIENTO WHERE $COLUMN_MOV_ID = ?", arrayOf(mov.id.toString()))
            if (cursor.moveToFirst()) {
                val deudaId = cursor.getString(0)
                val ahorroId = cursor.getString(1)
                
                if (deudaId != null) {
                    db.execSQL("UPDATE $TABLE_DEUDAS SET $COLUMN_D_PAGADO = $COLUMN_D_PAGADO - ? WHERE $COLUMN_D_ID = ?", arrayOf(mov.valor.toDouble(), deudaId))
                } else if (ahorroId != null) {
                    db.execSQL("UPDATE $TABLE_AHORROS SET $COLUMN_A_MONTO_ACTUAL = $COLUMN_A_MONTO_ACTUAL - ? WHERE $COLUMN_A_ID = ?", arrayOf(mov.valor.toDouble(), ahorroId))
                }
            }
            cursor.close()
            
            db.delete(TABLE_MOVIMIENTO, "$COLUMN_MOV_ID = ?", arrayOf(mov.id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            invalidateCache()
        }
    }

    fun marcarPagoManualControlAnual(gastoFijoId: String, anioMes: String, quincena: Int, esPagado: Boolean) {
        val db = writableDatabase
        if (esPagado) {
            val v = ContentValues().apply {
                put(COLUMN_CA_GF_ID, gastoFijoId)
                put(COLUMN_CA_ANIO_MES, anioMes)
                put(COLUMN_CA_QUINCENA, quincena)
                put(COLUMN_CA_PAGADO_MANUAL, 1)
            }
            db.insertWithOnConflict(TABLE_CONTROL_ANUAL, null, v, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            db.delete(TABLE_CONTROL_ANUAL, "$COLUMN_CA_GF_ID = ? AND $COLUMN_CA_ANIO_MES = ? AND $COLUMN_CA_QUINCENA = ?", arrayOf(gastoFijoId, anioMes, quincena.toString()))
        }
    }

    fun esPagoManualControlAnual(gastoFijoId: String, anioMes: String, quincena: Int): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_CONTROL_ANUAL WHERE $COLUMN_CA_GF_ID = ? AND $COLUMN_CA_ANIO_MES = ? AND $COLUMN_CA_QUINCENA = ?", arrayOf(gastoFijoId, anioMes, quincena.toString()))
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }
}
