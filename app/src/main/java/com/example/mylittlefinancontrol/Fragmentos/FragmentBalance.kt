package com.example.mylittlefinancontrol.Fragmentos

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylittlefinancontrol.Adaptadores.DeudasAdapter
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.DialogAgregarDeudaBinding
import com.example.mylittlefinancontrol.databinding.FragmentBalanceBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.NumberFormat
import java.util.*
import kotlin.math.max

class FragmentBalance : Fragment() {

    private var _binding: FragmentBalanceBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var adapter: DeudasAdapter
    private var listaDeudasTodas = mutableListOf<Deuda>()
    private var filtroTipoSeleccionado = "Todas"

    private var esSaldoOculto: Boolean = false
    private var saldoRealTexto: String = "0,00"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBalanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

        setupRecyclerView()
        setupFiltros()
        setupPrivacidad()
        cargarDeudas()

        binding.fabAgregarDeuda.setOnClickListener {
            mostrarDialogoDeuda(null)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDeudas()
    }

    private fun setupRecyclerView() {
        adapter = DeudasAdapter(emptyList(),
            onEditarClick = { deuda -> mostrarDialogoDeuda(deuda) },
            onEliminarClick = { deuda -> eliminarDeuda(deuda) },
            onAbonarClick = { deuda -> mostrarDialogoAbonarCuota(deuda) }
        )
        binding.rvDeudas.apply {
            layoutManager = LinearLayoutManager(mContext)
            adapter = this@FragmentBalance.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupPrivacidad() {
        binding.btnTogglePrivacidadDeudas.setOnClickListener {
            esSaldoOculto = !esSaldoOculto
            if (esSaldoOculto) {
                binding.tvTotalDeudasMonto.text = "••••••••"
                binding.btnTogglePrivacidadDeudas.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.tvTotalDeudasMonto.text = saldoRealTexto
                binding.btnTogglePrivacidadDeudas.setImageResource(R.drawable.ic_visibility)
            }
        }
    }

    private fun setupFiltros() {
        fun actualizarChipsUI(filtro: String) {
            filtroTipoSeleccionado = filtro
            val chipActivoBg = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val chipInactivoBg = ContextCompat.getColor(mContext, R.color.surface_card)
            val chipActivoText = ContextCompat.getColor(mContext, android.R.color.white)
            val chipInactivoText = ContextCompat.getColor(mContext, R.color.text_secondary)

            fun pintarChip(tv: TextView, activo: Boolean) {
                tv.backgroundTintList = ColorStateList.valueOf(if (activo) chipActivoBg else chipInactivoBg)
                tv.setTextColor(if (activo) chipActivoText else chipInactivoText)
            }

            pintarChip(binding.chipTodasDeudas, filtro == "Todas")
            pintarChip(binding.chipTarjetas, filtro == "Tarjeta")
            pintarChip(binding.chipPrestamos, filtro == "Préstamo")
            pintarChip(binding.chipPersonales, filtro == "Personal")

            aplicarFiltro()
        }

        binding.chipTodasDeudas.setOnClickListener { actualizarChipsUI("Todas") }
        binding.chipTarjetas.setOnClickListener { actualizarChipsUI("Tarjeta") }
        binding.chipPrestamos.setOnClickListener { actualizarChipsUI("Préstamo") }
        binding.chipPersonales.setOnClickListener { actualizarChipsUI("Personal") }
    }

    private fun aplicarFiltro() {
        val filtradas = if (filtroTipoSeleccionado == "Todas") {
            listaDeudasTodas
        } else {
            listaDeudasTodas.filter { it.tipo.contains(filtroTipoSeleccionado, ignoreCase = true) }
        }
        adapter.actualizarLista(filtradas)
    }

    private fun mostrarDialogoDeuda(deudaExistente: Deuda?) {
        val dialogBinding = DialogAgregarDeudaBinding.inflate(layoutInflater)
        val bottomSheetDialog = BottomSheetDialog(mContext, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(dialogBinding.root)

        var tipoSeleccionado = "Tarjeta de Crédito"
        var cuotasSeleccionadas = 12

        // Configuración de Chips de Tipos
        fun pintarChipTipo(tipo: String) {
            tipoSeleccionado = tipo
            val bgActivo = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val bgInactivo = ContextCompat.getColor(mContext, R.color.surface_card)
            val colorActivo = ContextCompat.getColor(mContext, android.R.color.white)
            val colorInactivo = ContextCompat.getColor(mContext, R.color.text_secondary)

            fun setChip(container: View, icon: View?, text: TextView, activo: Boolean) {
                container.backgroundTintList = ColorStateList.valueOf(if (activo) bgActivo else bgInactivo)
                text.setTextColor(if (activo) colorActivo else colorInactivo)
                if (icon is ImageView) {
                    icon.imageTintList = ColorStateList.valueOf(if (activo) colorActivo else colorInactivo)
                }
            }

            setChip(dialogBinding.chipTipoTarjeta, dialogBinding.chipTipoTarjeta.getChildAt(0), dialogBinding.chipTipoTarjeta.getChildAt(1) as TextView, tipo == "Tarjeta de Crédito")
            setChip(dialogBinding.chipTipoBancario, dialogBinding.chipTipoBancario.getChildAt(0), dialogBinding.chipTipoBancario.getChildAt(1) as TextView, tipo == "Préstamo Bancario")
            setChip(dialogBinding.chipTipoPersonal, dialogBinding.chipTipoPersonal.getChildAt(0), dialogBinding.chipTipoPersonal.getChildAt(1) as TextView, tipo == "Préstamo Personal")
            setChip(dialogBinding.chipTipoAuto, dialogBinding.chipTipoAuto.getChildAt(0), dialogBinding.chipTipoAuto.getChildAt(1) as TextView, tipo == "Crédito Automotriz")

            dialogBinding.autoTipoDeuda.setText(tipo, false)
        }

        dialogBinding.chipTipoTarjeta.setOnClickListener { pintarChipTipo("Tarjeta de Crédito") }
        dialogBinding.chipTipoBancario.setOnClickListener { pintarChipTipo("Préstamo Bancario") }
        dialogBinding.chipTipoPersonal.setOnClickListener { pintarChipTipo("Préstamo Personal") }
        dialogBinding.chipTipoAuto.setOnClickListener { pintarChipTipo("Crédito Automotriz") }

        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        // Cálculo dinámico de saldo restante y cuota estimada
        fun actualizarCalculos() {
            val total = dialogBinding.etTotalDeuda.text.toString().replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
            val pagado = dialogBinding.etTotalPagado.text.toString().replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
            val rest = max(0.0, total - pagado)
            dialogBinding.tvSaldoRestanteCalculado.text = "Saldo Restante: ${format.format(rest)}"

            val cuotaEst = if (cuotasSeleccionadas > 0 && total > 0) total / cuotasSeleccionadas else 0.0
            dialogBinding.tvCuotaEstimada.text = "Cuota est.: ${format.format(cuotaEst)} / mes"
        }

        // Botones de Cuotas (6x, 12x, 24x, Otro)
        fun pintarBotonCuotas(cuotas: Int) {
            cuotasSeleccionadas = cuotas
            val bgActivo = ContextCompat.getColor(mContext, R.color.surface_container_low)
            val bgInactivo = ContextCompat.getColor(mContext, R.color.surface_card)
            val colorActivo = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val colorInactivo = ContextCompat.getColor(mContext, R.color.text_secondary)

            fun setBtn(tv: TextView, activo: Boolean) {
                tv.backgroundTintList = ColorStateList.valueOf(if (activo) bgActivo else bgInactivo)
                tv.setTextColor(if (activo) colorActivo else colorInactivo)
            }

            setBtn(dialogBinding.btnCuota6x, cuotas == 6)
            setBtn(dialogBinding.btnCuota12x, cuotas == 12)
            setBtn(dialogBinding.btnCuota24x, cuotas == 24)

            actualizarCalculos()
        }

        dialogBinding.btnCuota6x.setOnClickListener { pintarBotonCuotas(6) }
        dialogBinding.btnCuota12x.setOnClickListener { pintarBotonCuotas(12) }
        dialogBinding.btnCuota24x.setOnClickListener { pintarBotonCuotas(24) }
        dialogBinding.etCuotasOtro.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val num = s?.toString()?.toIntOrNull()
                if (num != null && num > 0) {
                    cuotasSeleccionadas = num
                    pintarBotonCuotas(num)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { actualizarCalculos() }
        }
        dialogBinding.etTotalDeuda.addTextChangedListener(textWatcher)
        dialogBinding.etTotalPagado.addTextChangedListener(textWatcher)

        val setupDatePicker = { clickContainer: View, editText: EditText ->
            val datePickerAction = {
                val cal = Calendar.getInstance()
                DatePickerDialog(mContext, { _, year, month, day ->
                    val fecha = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                    editText.setText(fecha)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            clickContainer.setOnClickListener { datePickerAction() }
            editText.setOnClickListener { datePickerAction() }
        }
        setupDatePicker(dialogBinding.llFechaInicioClick, dialogBinding.etFechaInicio)
        setupDatePicker(dialogBinding.llFechaFinClick, dialogBinding.etFechaFin)

        if (deudaExistente != null) {
            dialogBinding.tvTituloDialogo.text = "Editar Deuda"
            dialogBinding.etConcepto.setText(deudaExistente.concepto)
            dialogBinding.etDescripcion.setText(deudaExistente.descripcion)
            dialogBinding.etTotalDeuda.setText(deudaExistente.totalDeuda.toInt().toString())
            dialogBinding.etTotalPagado.setText(deudaExistente.totalPagado.toInt().toString())
            dialogBinding.etFechaInicio.setText(deudaExistente.fechaInicio)
            dialogBinding.etFechaFin.setText(deudaExistente.fechaFin)
            pintarChipTipo(deudaExistente.tipo)
        } else {
            pintarChipTipo("Tarjeta de Crédito")
            pintarBotonCuotas(12)
        }
        actualizarCalculos()

        dialogBinding.btnCerrar.setOnClickListener { bottomSheetDialog.dismiss() }
        dialogBinding.btnCancelar.setOnClickListener { bottomSheetDialog.dismiss() }

        dialogBinding.btnGuardar.setOnClickListener {
            val tipo = tipoSeleccionado
            val concepto = dialogBinding.etConcepto.text.toString().trim()
            val descripcion = dialogBinding.etDescripcion.text.toString().trim()
            val total = dialogBinding.etTotalDeuda.text.toString().replace("$", "").trim().toDoubleOrNull() ?: 0.0
            val pagado = dialogBinding.etTotalPagado.text.toString().replace("$", "").trim().toDoubleOrNull() ?: 0.0
            val fechaIni = dialogBinding.etFechaInicio.text.toString().trim()
            val fechaFin = dialogBinding.etFechaFin.text.toString().trim()

            if (concepto.isNotEmpty() && total > 0) {
                if (deudaExistente == null) {
                    db.insertDeuda(Deuda("", tipo, concepto, descripcion, total, pagado, fechaIni, fechaFin))
                } else {
                    deudaExistente.tipo = tipo
                    deudaExistente.concepto = concepto
                    deudaExistente.descripcion = descripcion
                    deudaExistente.totalDeuda = total
                    deudaExistente.totalPagado = pagado
                    deudaExistente.fechaInicio = fechaIni
                    deudaExistente.fechaFin = fechaFin
                    db.updateDeuda(deudaExistente)
                }
                cargarDeudas()
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(mContext, "Por favor ingrese el concepto y el monto total", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.show()
    }

    private fun mostrarDialogoAbonarCuota(deuda: Deuda) {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val input = EditText(mContext).apply {
            hint = "Monto a abonar (Ej: 50000)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(48, 32, 48, 32)
        }

        val restante = (deuda.totalDeuda - deuda.totalPagado).coerceAtLeast(0.0)

        AlertDialog.Builder(mContext)
            .setTitle("Abonar a ${deuda.concepto}")
            .setMessage("Saldo restante actual: ${format.format(restante)}\nIngresa el valor de la cuota pagada:")
            .setView(input)
            .setPositiveButton("Registrar Abono") { _, _ ->
                val abono = input.text.toString().toDoubleOrNull() ?: 0.0
                if (abono > 0) {
                    deuda.totalPagado += abono
                    db.updateDeuda(deuda)

                    // Insertar también como movimiento financiero
                    val fecha = Constantes.formatoFecha(System.currentTimeMillis())
                    db.insertNota(Movimiento(0, "PAGO DEUDA: ${deuda.concepto.uppercase()}", abono.toInt(), "PAGO DEUDA", fecha))

                    Toast.makeText(mContext, "Abono registrado con éxito", Toast.LENGTH_SHORT).show()
                    cargarDeudas()
                } else {
                    Toast.makeText(mContext, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cargarDeudas() {
        listaDeudasTodas = db.getAllDeudas().toMutableList()
        aplicarFiltro()
        
        val totalDeudaSum = listaDeudasTodas.sumOf { it.totalDeuda }
        val totalPagadoSum = listaDeudasTodas.sumOf { it.totalPagado }
        val totalRestante = max(0.0, totalDeudaSum - totalPagadoSum)
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        saldoRealTexto = format.format(totalRestante).replace("$", "").trim()
        if (!esSaldoOculto) {
            binding.tvTotalDeudasMonto.text = saldoRealTexto
        }
        
        val progreso = if (totalDeudaSum > 0) {
            ((totalPagadoSum / totalDeudaSum) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        binding.pbAmortizacionTotal.progress = progreso
        binding.tvAmortizacionPagada.text = "$progreso% (${format.format(totalPagadoSum)} pagado)"
        binding.tvBadgeDeudas.text = "${listaDeudasTodas.size} pendientes"
        binding.tvResumenAbonado.text = "Abonado: ${format.format(totalPagadoSum)}"
        binding.tvResumenPendiente.text = "Pendiente: ${format.format(totalRestante)}"

        binding.chipTodasDeudas.text = "Todas (${listaDeudasTodas.size})"
        binding.chipTarjetas.text = "Tarjetas (${listaDeudasTodas.count { it.tipo.contains("tarjeta", true) }})"
        binding.chipPrestamos.text = "Préstamos (${listaDeudasTodas.count { it.tipo.contains("préstamo", true) || it.tipo.contains("prestamo", true) || it.tipo.contains("bancario", true) }})"
        binding.chipPersonales.text = "Personales (${listaDeudasTodas.count { it.tipo.contains("personal", true) }})"
    }

    private fun eliminarDeuda(deuda: Deuda) {
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Deuda")
            .setMessage("¿Estás seguro de que deseas eliminar este registro?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteDeuda(deuda.id)
                cargarDeudas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
