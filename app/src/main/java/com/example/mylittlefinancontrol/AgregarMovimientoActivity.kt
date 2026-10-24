package com.example.mylittlefinancontrol

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.databinding.ActivityAgregarMovimientoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AgregarMovimientoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgregarMovimientoBinding
    private lateinit var db: NotasDatabaseHelper
    private lateinit var firebaseAuth: FirebaseAuth
    private val listaDeudas = mutableListOf<Deuda>()
    private var deudaSeleccionada: Deuda? = null
    private val listaGastosFijos = mutableListOf<GastoFijo>()
    private var gastoFijoSeleccionado: GastoFijo? = null

    private var tipoSeleccionado = "GASTO"
    private var fechaSeleccionadaTimestamp = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarMovimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotasDatabaseHelper.getInstance(this)
        firebaseAuth = FirebaseAuth.getInstance()

        setupSegmentedTabs()
        setupCategoryChips()
        setupDatePicker()
        cargarDeudasLocales()

        binding.cbEsPagoDeuda.setOnCheckedChangeListener { _, isChecked ->
            binding.tilSeleccionDeuda.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                binding.cbEsGastoFijo.isChecked = false
                gastoFijoSeleccionado = null
                binding.tilSeleccionGastoFijo.visibility = View.GONE
            }
        }

        binding.cbEsGastoFijo.setOnCheckedChangeListener { _, isChecked ->
            binding.tilSeleccionGastoFijo.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                binding.cbEsPagoDeuda.isChecked = false
                deudaSeleccionada = null
                binding.tilSeleccionDeuda.visibility = View.GONE
            }
        }

        binding.autoDeudas.setOnItemClickListener { _, _, position, _ ->
            if (position < listaDeudas.size) {
                deudaSeleccionada = listaDeudas[position]
                binding.etDescripcion.setText("PAGO DEUDA: ${deudaSeleccionada?.concepto}")
            }
        }

        binding.autoGastosFijos.setOnItemClickListener { _, _, position, _ ->
            if (position < listaGastosFijos.size) {
                gastoFijoSeleccionado = listaGastosFijos[position]
                binding.etDescripcion.setText("PAGO: ${gastoFijoSeleccionado?.concepto}")
                binding.etValor.setText(gastoFijoSeleccionado?.monto?.toInt().toString())
            }
        }

        binding.ivGuardarMovimiento.setOnClickListener {
            validarYGuardar()
        }

        binding.btnCancelarActivity.setOnClickListener {
            finish()
        }

        binding.btnEscanearActivity.setOnClickListener {
            Toast.makeText(this, "Abre la cámara desde el inicio o movimientos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSegmentedTabs() {
        fun actualizarTabsTipo(tipo: String) {
            tipoSeleccionado = tipo
            val colorGasto = ContextCompat.getColor(this, R.color.expense_red)
            val colorIngreso = ContextCompat.getColor(this, R.color.primary_emerald)
            val colorTransferencia = ContextCompat.getColor(this, R.color.text_primary)
            val colorInactivo = ContextCompat.getColor(this, R.color.text_secondary)
            val bgActivo = ContextCompat.getColor(this, R.color.surface_card)
            val bgInactivo = ContextCompat.getColor(this, android.R.color.transparent)

            binding.btnTipoGasto.setBackgroundColor(if (tipo == "GASTO") bgActivo else bgInactivo)
            binding.btnTipoGasto.setTextColor(if (tipo == "GASTO") colorGasto else colorInactivo)
            binding.btnTipoGasto.elevation = if (tipo == "GASTO") 4f else 0f

            binding.btnTipoIngreso.setBackgroundColor(if (tipo == "INGRESO") bgActivo else bgInactivo)
            binding.btnTipoIngreso.setTextColor(if (tipo == "INGRESO") colorIngreso else colorInactivo)
            binding.btnTipoIngreso.elevation = if (tipo == "INGRESO") 4f else 0f

            binding.btnTipoTransferencia.setBackgroundColor(if (tipo == "TRANSFERENCIA") bgActivo else bgInactivo)
            binding.btnTipoTransferencia.setTextColor(if (tipo == "TRANSFERENCIA") colorTransferencia else colorInactivo)
            binding.btnTipoTransferencia.elevation = if (tipo == "TRANSFERENCIA") 4f else 0f

            binding.autoTipoMov.setText(tipo, false)
        }

        binding.btnTipoGasto.setOnClickListener { actualizarTabsTipo("GASTO") }
        binding.btnTipoIngreso.setOnClickListener { actualizarTabsTipo("INGRESO") }
        binding.btnTipoTransferencia.setOnClickListener { actualizarTabsTipo("TRANSFERENCIA") }
    }

    private fun setupCategoryChips() {
        val listaCategorias = db.obtenerCategoriasPorModulo("MOVIMIENTO")
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaCategorias)
        binding.autoCategoriaMovimiento.setAdapter(catAdapter)
        if (listaCategorias.isNotEmpty()) {
            binding.autoCategoriaMovimiento.setText(listaCategorias.first(), false)
        }

        binding.autoCategoriaMovimiento.setOnItemClickListener { _, _, position, _ ->
            val seleccion = catAdapter.getItem(position) ?: ""
            if (seleccion == "Otra...") {
                binding.tilNuevaCategoriaMovimiento.visibility = View.VISIBLE
                binding.etNuevaCategoriaMovimiento.setText("")
                binding.etNuevaCategoriaMovimiento.requestFocus()
            } else {
                binding.tilNuevaCategoriaMovimiento.visibility = View.GONE
                binding.tilNuevaCategoriaMovimiento.error = null
            }
        }
    }

    private fun setupDatePicker() {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd MMM, HH:mm 'hrs'", Locale("es", "ES"))
        binding.tvFechaAct.text = "Hoy, ${String.format("%02d:%02d hrs", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}"

        binding.llFechaSelectorAct.setOnClickListener {
            val c = Calendar.getInstance().apply { timeInMillis = fechaSeleccionadaTimestamp }
            DatePickerDialog(this, { _, y, m, d ->
                c.set(Calendar.YEAR, y)
                c.set(Calendar.MONTH, m)
                c.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(this, { _, hour, min ->
                    c.set(Calendar.HOUR_OF_DAY, hour)
                    c.set(Calendar.MINUTE, min)
                    fechaSeleccionadaTimestamp = c.timeInMillis
                    binding.tvFechaAct.text = sdf.format(c.time)
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun cargarDeudasLocales() {
        lifecycleScope.launch(Dispatchers.IO) {
            val deudasTodas = db.getAllDeudas()
            val deudasPendientes = deudasTodas.filter { (it.totalDeuda - it.totalPagado) > 0.0 }
            val gfTodas = db.getAllGastosFijos()
            val metodosPagoList = db.obtenerMetodosPago()
            
            withContext(Dispatchers.Main) {
                listaDeudas.clear()
                listaDeudas.addAll(deudasPendientes)
                val nombresDeudas = listaDeudas.map { "${it.concepto} (Resta: $${it.totalDeuda - it.totalPagado})" }
                binding.autoDeudas.setAdapter(ArrayAdapter(this@AgregarMovimientoActivity, android.R.layout.simple_list_item_1, nombresDeudas))

                listaGastosFijos.clear()
                listaGastosFijos.addAll(gfTodas)
                val nombresGastos = listaGastosFijos.map { "${it.concepto} ($${it.monto})" }
                binding.autoGastosFijos.setAdapter(ArrayAdapter(this@AgregarMovimientoActivity, android.R.layout.simple_list_item_1, nombresGastos))

                // Cargar métodos de pago
                val nombresMetodos = metodosPagoList.map { it.nombreFormateado() }
                binding.autoMetodoPagoMovimiento.setAdapter(ArrayAdapter(this@AgregarMovimientoActivity, android.R.layout.simple_list_item_1, nombresMetodos))
                val metodoPorDefecto = metodosPagoList.find { it.esPredeterminado } ?: metodosPagoList.firstOrNull()
                binding.autoMetodoPagoMovimiento.setText(metodoPorDefecto?.nombreFormateado() ?: "Efectivo", false)
            }
        }
    }

    private fun validarYGuardar() {
        val descripcion = binding.etDescripcion.text.toString()
        val valorStr = binding.etValor.text.toString()
        val esPagoDeuda = binding.cbEsPagoDeuda.isChecked
        val esGastoFijo = binding.cbEsGastoFijo.isChecked
        val fecha = Constantes.formatoFecha(fechaSeleccionadaTimestamp)

        if (descripcion.isEmpty() || valorStr.isEmpty()) {
            Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val catSel = binding.autoCategoriaMovimiento.text.toString().trim()
        var categoriaFinal = catSel

        if (catSel == "Otra...") {
            val nuevaCatInput = binding.etNuevaCategoriaMovimiento.text.toString().trim()
            if (nuevaCatInput.isBlank()) {
                binding.tilNuevaCategoriaMovimiento.error = "Escribe el nombre de la nueva categoría"
                return
            }
            if (nuevaCatInput.equals("Otra...", ignoreCase = true)) {
                binding.tilNuevaCategoriaMovimiento.error = "Nombre de categoría inválido"
                return
            }
            binding.tilNuevaCategoriaMovimiento.error = null
            db.insertarCategoria(nuevaCatInput, "MOVIMIENTO")
            categoriaFinal = nuevaCatInput.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else if (catSel.isBlank()) {
            categoriaFinal = "Varios"
        }

        val valor = valorStr.toIntOrNull() ?: 0
        val metodoPagoSel = binding.autoMetodoPagoMovimiento.text.toString().ifBlank { "Efectivo" }

        fun ejecutarGuardado() {
            lifecycleScope.launch(Dispatchers.IO) {
                if (esPagoDeuda) {
                    if (deudaSeleccionada == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AgregarMovimientoActivity, "Seleccione una deuda", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    deudaSeleccionada!!.totalPagado += valor
                    db.updateDeuda(deudaSeleccionada!!)
                    actualizarDeudaEnFirebase(deudaSeleccionada!!, valor)
                    guardarMovimento(descripcion.uppercase(), valor, "PAGO DEUDA", fecha, categoria = categoriaFinal, metodoPago = metodoPagoSel)
                } else if (esGastoFijo && gastoFijoSeleccionado != null) {
                    val gf = gastoFijoSeleccionado!!
                    val mesAnio = fecha.substring(0, 7)
                    gf.ultimoPagoQ1 = mesAnio
                    db.updateGastoFijo(gf)
                    guardarMovimento(descripcion.uppercase(), valor, "GASTO FIJO", fecha, gf.id.toInt(), 0, categoria = categoriaFinal, metodoPago = metodoPagoSel)
                } else {
                    guardarMovimento(descripcion.uppercase(), valor, tipoSeleccionado.uppercase(), fecha, categoria = categoriaFinal, metodoPago = metodoPagoSel)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val duplicado = db.buscarMovimientoDuplicado(valor, fecha)
            withContext(Dispatchers.Main) {
                if (duplicado != null) {
                    val format = java.text.NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                    val valorFmt = format.format(duplicado.valor)
                    var fechaDisplay = duplicado.fecha
                    var horaDisplay = ""
                    val ts = NotasDatabaseHelper.obtenerTimestampParaOrdenamiento(duplicado)
                    if (ts > 0L) {
                        val cal = Calendar.getInstance().apply { timeInMillis = ts }
                        fechaDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(cal.time)
                        horaDisplay = SimpleDateFormat("HH:mm", Locale.US).format(cal.time)
                    }

                    val mensaje = if (horaDisplay.isNotBlank()) {
                        "El día $fechaDisplay a las $horaDisplay ya se registró un movimiento por $valorFmt (${duplicado.descripcion}). ¿Es el mismo movimiento?"
                    } else {
                        "El día $fechaDisplay ya se registró un movimiento por $valorFmt (${duplicado.descripcion}). ¿Es el mismo movimiento?"
                    }

                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this@AgregarMovimientoActivity)
                        .setTitle("Movimiento Existente Detectado")
                        .setMessage(mensaje)
                        .setPositiveButton("Guardar de todos modos") { _, _ -> ejecutarGuardado() }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    ejecutarGuardado()
                }
            }
        }
    }

    private fun actualizarDeudaEnFirebase(deuda: Deuda, abono: Int) {
        val uid = firebaseAuth.uid ?: return
        val nuevoPagado = deuda.totalPagado + abono
        FirebaseDatabase.getInstance().getReference("Deudas")
            .child(uid)
            .child(deuda.id)
            .child("totalPagado")
            .setValue(nuevoPagado)
    }

    private fun guardarMovimento(descripcion: String, valor: Int, tipo_reg: String, fecha: String, gastoFijoId: Int? = null, quincena: Int? = null, categoria: String = "Varios", metodoPago: String = "Efectivo") {
        val fc = Constantes.formatoFecha(System.currentTimeMillis())
        val movimiento = Movimiento(0, descripcion, valor, tipo_reg, fecha, gastoFijoId, quincena, categoria = categoria, fechaCreacion = fc, metodoPago = metodoPago)
        db.insertNota(movimiento)
        runOnUiThread {
            Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
