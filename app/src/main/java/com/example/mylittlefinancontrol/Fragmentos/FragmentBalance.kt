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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        db = NotasDatabaseHelper.getInstance(mContext)

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
        val porTipo = if (filtroTipoSeleccionado == "Todas") {
            listaDeudasTodas
        } else {
            listaDeudasTodas.filter { it.tipo.contains(filtroTipoSeleccionado, ignoreCase = true) }
        }

        // Ordenar: Deudas pendientes PRIMERO, deudas completadas (✓ PAGADA) AL FINAL
        val ordenadas = porTipo.sortedWith(
            compareBy<Deuda> { (it.totalDeuda - it.totalPagado) <= 0.0 }
                .thenBy { it.concepto }
        )

        adapter.actualizarLista(ordenadas)
    }

    private fun mostrarDialogoDeuda(deudaExistente: Deuda?) {
        if (deudaExistente != null && (deudaExistente.totalDeuda - deudaExistente.totalPagado) <= 0.0) {
            Toast.makeText(mContext, "Las deudas completadas no se pueden editar", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogAgregarDeudaBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(mContext)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var cuotasSeleccionadas = 12

        // Configuración de Tipo de Compromiso Dinámico
        val listaTiposDeuda = db.obtenerCategoriasPorModulo("DEUDA")
        val catAdapterDeuda = ArrayAdapter(mContext, android.R.layout.simple_list_item_1, listaTiposDeuda)
        dialogBinding.autoTipoDeuda.setAdapter(catAdapterDeuda)

        val tipoInicial = deudaExistente?.tipo ?: if (listaTiposDeuda.isNotEmpty()) listaTiposDeuda.first() else "Tarjeta de Crédito"
        dialogBinding.autoTipoDeuda.setText(tipoInicial, false)

        dialogBinding.autoTipoDeuda.setOnItemClickListener { _, _, position, _ ->
            val seleccion = catAdapterDeuda.getItem(position) ?: ""
            if (seleccion == "Otra...") {
                dialogBinding.tilNuevoTipoDeuda.visibility = View.VISIBLE
                dialogBinding.etNuevoTipoDeuda.setText("")
                dialogBinding.etNuevoTipoDeuda.requestFocus()
            } else {
                dialogBinding.tilNuevoTipoDeuda.visibility = View.GONE
                dialogBinding.tilNuevoTipoDeuda.error = null
            }
        }

        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        var tipoInteres = "Anual"
        var totalDeudaRealCalculado = 0.0
        var valorCuotaCalculada = 0.0

        // Cálculo dinámico de saldo restante, intereses y cuota estimada
        fun actualizarCalculos() {
            val capitalBase = Constantes.limpiarYParsearMonto(dialogBinding.etTotalDeuda.text.toString())
            val pagado = Constantes.limpiarYParsearMonto(dialogBinding.etTotalPagado.text.toString())
            val tasaNum = dialogBinding.etTasaInteres.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            val cuotaManual = Constantes.limpiarYParsearMonto(dialogBinding.etValorCuotaManual.text.toString())

            val n = if (cuotasSeleccionadas > 0) cuotasSeleccionadas else 1

            if (cuotaManual > 0.0) {
                valorCuotaCalculada = Constantes.redondearMonto(cuotaManual)
                totalDeudaRealCalculado = Constantes.redondearMonto(cuotaManual * n)
            } else if (tasaNum > 0.0 && capitalBase > 0.0) {
                val rateMonthly = if (tipoInteres == "Anual") (tasaNum / 1200.0) else (tasaNum / 100.0)
                if (rateMonthly > 0) {
                    val factor = Math.pow(1.0 + rateMonthly, n.toDouble())
                    valorCuotaCalculada = Constantes.redondearMonto(capitalBase * (rateMonthly * factor) / (factor - 1.0))
                } else {
                    valorCuotaCalculada = Constantes.redondearMonto(capitalBase / n)
                }
                totalDeudaRealCalculado = Constantes.redondearMonto(valorCuotaCalculada * n)
            } else {
                valorCuotaCalculada = if (capitalBase > 0) Constantes.redondearMonto(capitalBase / n) else 0.0
                totalDeudaRealCalculado = Constantes.redondearMonto(capitalBase)
            }

            val interesTotal = max(0.0, totalDeudaRealCalculado - capitalBase)
            val saldoRestante = max(0.0, totalDeudaRealCalculado - pagado)

            dialogBinding.tvCuotaEstimada.text = "Cuota est.: ${format.format(valorCuotaCalculada)} / mes"
            dialogBinding.tvSaldoRestanteCalculado.text = "Saldo Restante: ${format.format(saldoRestante)}"
            dialogBinding.tvResumenTotalInteres.text = "Interés estimado: ${format.format(interesTotal)} (${String.format(Locale.US, "%.2f", tasaNum)}% $tipoInteres)"
            dialogBinding.tvResumenTotalDeudaReal.text = "Total real a pagar: ${format.format(totalDeudaRealCalculado)}"
        }

        // Configuración de conmutador de Tasa Anual / Mensual
        dialogBinding.tvTipoInteresBadge.setOnClickListener {
            tipoInteres = if (tipoInteres == "Anual") "Mensual" else "Anual"
            dialogBinding.tvTipoInteresBadge.text = "% $tipoInteres"
            actualizarCalculos()
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
        dialogBinding.etTasaInteres.addTextChangedListener(textWatcher)
        dialogBinding.etValorCuotaManual.addTextChangedListener(textWatcher)

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
            dialogBinding.etTotalDeuda.setText(deudaExistente.totalDeuda.toLong().toString())
            dialogBinding.etTotalPagado.setText(deudaExistente.totalPagado.toLong().toString())
            dialogBinding.etFechaInicio.setText(deudaExistente.fechaInicio)
            dialogBinding.etFechaFin.setText(deudaExistente.fechaFin)

            tipoInteres = if (deudaExistente.tipoInteres.isNotBlank()) deudaExistente.tipoInteres else "Anual"
            dialogBinding.tvTipoInteresBadge.text = "% $tipoInteres"
            if (deudaExistente.tasaInteres > 0) {
                dialogBinding.etTasaInteres.setText(deudaExistente.tasaInteres.toString())
            }
            if (deudaExistente.valorCuota > 0) {
                dialogBinding.etValorCuotaManual.setText(deudaExistente.valorCuota.toLong().toString())
            }
            dialogBinding.autoTipoDeuda.setText(deudaExistente.tipo, false)
            if (deudaExistente.cuotas > 0) {
                pintarBotonCuotas(deudaExistente.cuotas)
            }
        } else {
            pintarBotonCuotas(12)
        }
        actualizarCalculos()

        dialogBinding.btnCerrar.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnGuardar.setOnClickListener {
            val catSel = dialogBinding.autoTipoDeuda.text.toString().trim()
            var tipoFinal = catSel

            if (catSel == "Otra...") {
                val nuevoTipoInput = dialogBinding.etNuevoTipoDeuda.text.toString().trim()
                if (nuevoTipoInput.isBlank()) {
                    dialogBinding.tilNuevoTipoDeuda.error = "Escribe el nuevo tipo de compromiso"
                    return@setOnClickListener
                }
                if (nuevoTipoInput.equals("Otra...", ignoreCase = true)) {
                    dialogBinding.tilNuevoTipoDeuda.error = "Tipo de compromiso inválido"
                    return@setOnClickListener
                }
                dialogBinding.tilNuevoTipoDeuda.error = null
                db.insertarCategoria(nuevoTipoInput, "DEUDA")
                tipoFinal = nuevoTipoInput.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } else if (catSel.isBlank()) {
                tipoFinal = "Préstamo"
            }

            val concepto = dialogBinding.etConcepto.text.toString().trim()
            val descripcion = dialogBinding.etDescripcion.text.toString().trim()
            val capitalInput = Constantes.limpiarYParsearMonto(dialogBinding.etTotalDeuda.text.toString())
            val totalFinal = if (totalDeudaRealCalculado > 0.0) totalDeudaRealCalculado else capitalInput
            val pagado = Constantes.limpiarYParsearMonto(dialogBinding.etTotalPagado.text.toString())
            val tasaNum = dialogBinding.etTasaInteres.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            val fechaIni = dialogBinding.etFechaInicio.text.toString().trim()
            val fechaFin = dialogBinding.etFechaFin.text.toString().trim()

            if (concepto.isNotEmpty() && totalFinal > 0) {
                if (deudaExistente == null) {
                    db.insertDeuda(Deuda("", tipoFinal, concepto, descripcion, totalFinal, pagado, fechaIni, fechaFin, tasaNum, tipoInteres, cuotasSeleccionadas, valorCuotaCalculada))
                    cargarDeudas()
                    
                    MaterialAlertDialogBuilder(mContext)
                        .setTitle("¡Guardado con éxito!")
                        .setMessage("¿Quieres agregar otro compromiso financiero?")
                        .setPositiveButton("Sí, agregar otro") { _, _ ->
                            dialogBinding.etConcepto.setText("")
                            dialogBinding.etDescripcion.setText("")
                            dialogBinding.etTotalDeuda.setText("")
                            dialogBinding.etTotalPagado.setText("")
                            dialogBinding.etConcepto.requestFocus()
                        }
                        .setNegativeButton("No, terminar") { _, _ ->
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    deudaExistente.tipo = tipoFinal
                    deudaExistente.concepto = concepto
                    deudaExistente.descripcion = descripcion
                    deudaExistente.totalDeuda = totalFinal
                    deudaExistente.totalPagado = pagado
                    deudaExistente.fechaInicio = fechaIni
                    deudaExistente.fechaFin = fechaFin
                    deudaExistente.tasaInteres = tasaNum
                    deudaExistente.tipoInteres = tipoInteres
                    deudaExistente.cuotas = cuotasSeleccionadas
                    deudaExistente.valorCuota = valorCuotaCalculada
                    db.updateDeuda(deudaExistente)
                    cargarDeudas()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(mContext, "Por favor ingrese el concepto y el monto total", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun mostrarDialogoAbonarCuota(deuda: Deuda) {
        val dialogBinding = com.example.mylittlefinancontrol.databinding.DialogAbonarDeudaBinding.inflate(layoutInflater)
        val dialog = android.app.Dialog(mContext)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val restante = (deuda.totalDeuda - deuda.totalPagado).coerceAtLeast(0.0)

        dialogBinding.tvTituloAbonar.text = "Abonar a ${deuda.concepto}"
        dialogBinding.tvSaldoRestante.text = "Saldo restante actual: ${format.format(restante)}"

        dialogBinding.btnCancelarAbono.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnRegistrarAbono.setOnClickListener {
            val abonoStr = dialogBinding.etMontoAbono.text.toString()
            val abono = abonoStr.toDoubleOrNull() ?: 0.0

            if (abono > 0) {
                // Insertar como movimiento financiero y afectar la deuda transaccionalmente
                val fecha = Constantes.formatoFecha(System.currentTimeMillis())
                val mov = Movimiento(0, "PAGO DEUDA: ${deuda.concepto.uppercase()}", abono.toInt(), "PAGO DEUDA", fecha, categoria = "Pago Deuda")
                db.insertarAbonoDeudaTransaccional(mov, deuda.id)

                Toast.makeText(mContext, "Abono registrado con éxito", Toast.LENGTH_SHORT).show()
                cargarDeudas()
                
                MaterialAlertDialogBuilder(mContext)
                    .setTitle("¡Guardado con éxito!")
                    .setMessage("¿Quieres agregar otro registro a esta u otra deuda?")
                    .setPositiveButton("Sí, agregar otro") { _, _ ->
                        dialogBinding.etMontoAbono.setText("")
                        dialogBinding.etMontoAbono.requestFocus()
                    }
                    .setNegativeButton("No, terminar") { _, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } else {
                Toast.makeText(mContext, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun cargarDeudas() {
        lifecycleScope.launch(Dispatchers.IO) {
            val lista = db.getAllDeudas().toMutableList()
            withContext(Dispatchers.Main) {
                listaDeudasTodas = lista
                val deudasPendientes = listaDeudasTodas.filter { (it.totalDeuda - it.totalPagado) > 0.0 }
                aplicarFiltro()
                
                val totalDeudaSum = deudasPendientes.sumOf { it.totalDeuda }
                val totalPagadoSum = deudasPendientes.sumOf { it.totalPagado }
                val totalRestante = deudasPendientes.sumOf { (it.totalDeuda - it.totalPagado).coerceAtLeast(0.0) }
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
                binding.tvBadgeDeudas.text = "${deudasPendientes.size} pendientes"
                binding.tvResumenAbonado.text = "Abonado: ${format.format(totalPagadoSum)}"
                binding.tvResumenPendiente.text = "Pendiente: ${format.format(totalRestante)}"

                binding.chipTodasDeudas.text = "Todas (${listaDeudasTodas.size})"
                binding.chipTarjetas.text = "Tarjetas (${listaDeudasTodas.count { it.tipo.contains("tarjeta", true) }})"
                binding.chipPrestamos.text = "Préstamos (${listaDeudasTodas.count { it.tipo.contains("préstamo", true) || it.tipo.contains("prestamo", true) || it.tipo.contains("bancario", true) }})"
                binding.chipPersonales.text = "Personales (${listaDeudasTodas.count { it.tipo.contains("personal", true) }})"
            }
        }
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
