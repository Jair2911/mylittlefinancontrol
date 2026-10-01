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

        db = NotasDatabaseHelper(this)
        firebaseAuth = FirebaseAuth.getInstance()

        setupSegmentedTabs()
        setupCategoryChips()
        setupDatePicker()
        setupPaymentMethodSelector()
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
        fun pintarChip(categoria: String) {
            val bgActivo = ContextCompat.getColor(this, R.color.surface_container_low)
            val bgInactivo = ContextCompat.getColor(this, R.color.surface_card)
            val colorActivo = ContextCompat.getColor(this, R.color.primary_emerald)
            val colorInactivo = ContextCompat.getColor(this, R.color.text_secondary)

            fun setChipStyle(container: View, text: TextView, activo: Boolean) {
                container.backgroundTintList = ColorStateList.valueOf(if (activo) bgActivo else bgInactivo)
                text.setTextColor(if (activo) colorActivo else colorInactivo)
            }

            val tvSuper = binding.chipSupermercadoAct.getChildAt(1) as TextView
            val tvServ = binding.chipServiciosAct.getChildAt(1) as TextView
            val tvComida = binding.chipComidaAct.getChildAt(1) as TextView
            val tvTransp = binding.chipTransporteAct.getChildAt(1) as TextView

            setChipStyle(binding.chipSupermercadoAct, tvSuper, categoria == "Supermercado")
            setChipStyle(binding.chipServiciosAct, tvServ, categoria == "Servicios")
            setChipStyle(binding.chipComidaAct, tvComida, categoria == "Comida")
            setChipStyle(binding.chipTransporteAct, tvTransp, categoria == "Transporte")
        }

        binding.chipSupermercadoAct.setOnClickListener {
            pintarChip("Supermercado")
            if (binding.etDescripcion.text.isNullOrEmpty() || binding.etDescripcion.text.toString().contains("Servicios|Comida|Transporte".toRegex())) {
                binding.etDescripcion.setText("Supermercado")
            }
        }
        binding.chipServiciosAct.setOnClickListener {
            pintarChip("Servicios")
            if (binding.etDescripcion.text.isNullOrEmpty() || binding.etDescripcion.text.toString().contains("Supermercado|Comida|Transporte".toRegex())) {
                binding.etDescripcion.setText("Servicios")
            }
        }
        binding.chipComidaAct.setOnClickListener {
            pintarChip("Comida")
            if (binding.etDescripcion.text.isNullOrEmpty() || binding.etDescripcion.text.toString().contains("Supermercado|Servicios|Transporte".toRegex())) {
                binding.etDescripcion.setText("Comida")
            }
        }
        binding.chipTransporteAct.setOnClickListener {
            pintarChip("Transporte")
            if (binding.etDescripcion.text.isNullOrEmpty() || binding.etDescripcion.text.toString().contains("Supermercado|Servicios|Comida".toRegex())) {
                binding.etDescripcion.setText("Transporte")
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

    private fun setupPaymentMethodSelector() {
        val metodos = arrayOf("Débito •••• 4021", "Tarjeta Visa", "Efectivo", "Banco Estado", "Transferencia")
        binding.llMetodoPagoAct.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Seleccionar Método de Pago")
                .setItems(metodos) { _, which ->
                    binding.tvMetodoPagoAct.text = metodos[which]
                }
                .show()
        }
    }

    private fun cargarDeudasLocales() {
        listaDeudas.clear()
        val deudasTodas = db.getAllDeudas()
        listaDeudas.addAll(deudasTodas.filter { (it.totalDeuda - it.totalPagado) > 0.0 })
        val nombresDeudas = listaDeudas.map { "${it.concepto} (Resta: $${it.totalDeuda - it.totalPagado})" }
        binding.autoDeudas.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, nombresDeudas))

        listaGastosFijos.clear()
        listaGastosFijos.addAll(db.getAllGastosFijos())
        val nombresGastos = listaGastosFijos.map { "${it.concepto} ($${it.monto})" }
        binding.autoGastosFijos.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, nombresGastos))
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

        val valor = valorStr.toIntOrNull() ?: 0

        if (esPagoDeuda) {
            if (deudaSeleccionada == null) {
                Toast.makeText(this, "Seleccione una deuda", Toast.LENGTH_SHORT).show()
                return
            }
            deudaSeleccionada!!.totalPagado += valor
            db.updateDeuda(deudaSeleccionada!!)
            actualizarDeudaEnFirebase(deudaSeleccionada!!, valor)
            guardarMovimento(descripcion.uppercase(), valor, "PAGO DEUDA", fecha)
        } else if (esGastoFijo && gastoFijoSeleccionado != null) {
            val gf = gastoFijoSeleccionado!!
            val mesAnio = fecha.substring(0, 7)
            gf.ultimoPagoQ1 = mesAnio
            db.updateGastoFijo(gf)
            guardarMovimento(descripcion.uppercase(), valor, "GASTO FIJO", fecha, gf.id.toInt(), 0)
        } else {
            guardarMovimento(descripcion.uppercase(), valor, tipoSeleccionado.uppercase(), fecha)
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

    private fun guardarMovimento(descripcion: String, valor: Int, tipo_reg: String, fecha: String, gastoFijoId: Int? = null, quincena: Int? = null) {
        val movimiento = Movimiento(0, descripcion, valor, tipo_reg, fecha, gastoFijoId, quincena)
        db.insertNota(movimiento)
        Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_SHORT).show()
        finish()
    }
}
