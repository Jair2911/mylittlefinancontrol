package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylittlefinancontrol.Adaptadores.GastosFijosAdapter
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.DialogAgregarGastoFijoBinding
import com.example.mylittlefinancontrol.databinding.FragmentGastosFijosBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.*

class FragmentGastosFijos : Fragment() {

    private var _binding: FragmentGastosFijosBinding? = null
    private val binding get() = _binding!!

    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var adapter: GastosFijosAdapter
    private var listaGastosTodos = mutableListOf<GastoFijo>()
    private var categoriaFiltroSeleccionada = "Todos"
    private var esOculto = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGastosFijosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

        setupRecyclerView()
        setupFiltros()
        cargarGastosFijos()

        binding.fabAgregar.setOnClickListener {
            mostrarDialogoGastoFijo(null)
        }

        binding.btnPrivacyToggleGf.setOnClickListener {
            esOculto = !esOculto
            actualizarResumenVisual()
        }

        binding.btnCalendarGf.setOnClickListener {
            Toast.makeText(mContext, "Calendario de compromisos del mes", Toast.LENGTH_SHORT).show()
        }

        binding.btnFilterGf.setOnClickListener {
            Toast.makeText(mContext, "Filtrando compromisos...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarGastosFijos()
    }

    private fun setupRecyclerView() {
        adapter = GastosFijosAdapter(
            emptyList(),
            onEditarClick = { gasto -> mostrarDialogoGastoFijo(gasto) },
            onEliminarClick = { gasto -> eliminarGastoFijo(gasto) }
        )
        binding.rvGastosFijos.apply {
            layoutManager = LinearLayoutManager(mContext)
            adapter = this@FragmentGastosFijos.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFiltros() {
        binding.cgCategoriasGf.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedId = checkedIds[0]
            categoriaFiltroSeleccionada = when (selectedId) {
                R.id.chip_servicios -> "Servicios"
                R.id.chip_vivienda -> "Vivienda"
                R.id.chip_telecom -> "Telecom"
                R.id.chip_suscripciones -> "Streaming"
                R.id.chip_educacion -> "Educación"
                R.id.chip_otros -> "Otros"
                else -> "Todos"
            }
            aplicarFiltro()
        }
    }

    private fun aplicarFiltro() {
        val filtrados = when (categoriaFiltroSeleccionada) {
            "Todos" -> listaGastosTodos
            "Servicios" -> listaGastosTodos.filter { it.categoria.contains("serv", true) || it.categoria.contains("luz", true) || it.categoria.contains("agua", true) }
            "Vivienda" -> listaGastosTodos.filter { it.categoria.contains("viv", true) || it.categoria.contains("arriend", true) || it.categoria.contains("casa", true) }
            "Telecom" -> listaGastosTodos.filter { it.categoria.contains("tele", true) || it.categoria.contains("inter", true) || it.categoria.contains("fibra", true) || it.categoria.contains("wifi", true) }
            "Streaming" -> listaGastosTodos.filter { it.categoria.contains("stream", true) || it.categoria.contains("suscrip", true) }
            "Educación" -> listaGastosTodos.filter { it.categoria.contains("educ", true) || it.categoria.contains("univ", true) || it.categoria.contains("col", true) }
            "Otros" -> listaGastosTodos.filter {
                !it.categoria.contains("serv", true) &&
                !it.categoria.contains("viv", true) &&
                !it.categoria.contains("tele", true) &&
                !it.categoria.contains("stream", true) &&
                !it.categoria.contains("educ", true)
            }
            else -> listaGastosTodos
        }
        adapter.actualizarLista(filtrados)
    }

    private fun mostrarDialogoGastoFijo(gastoExistente: GastoFijo?) {
        val bottomSheetDialog = BottomSheetDialog(mContext, R.style.BottomSheetDialogTheme)
        val dialogBinding = DialogAgregarGastoFijoBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(dialogBinding.root)

        var categoriaSeleccionada = "Telecom"

        // Helper para pintar la selección de las 6 tarjetas de categorías
        fun actualizarSeleccionCategoria(categoria: String) {
            categoriaSeleccionada = categoria

            val colorSelectedBg = ContextCompat.getColor(mContext, R.color.surface_container_low)
            val colorUnselectedBg = ContextCompat.getColor(mContext, R.color.surface_bg)
            val colorSelectedStroke = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val colorUnselectedStroke = ContextCompat.getColor(mContext, R.color.outline_variant)
            val colorSelectedText = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val colorUnselectedText = ContextCompat.getColor(mContext, R.color.text_secondary)

            fun configurarTarjeta(
                card: MaterialCardView,
                iv: ImageView,
                tv: TextView,
                nombre: String
            ) {
                val esActiva = categoria.equals(nombre, ignoreCase = true)
                card.setCardBackgroundColor(if (esActiva) colorSelectedBg else colorUnselectedBg)
                card.strokeColor = if (esActiva) colorSelectedStroke else colorUnselectedStroke
                card.strokeWidth = if (esActiva) (1.5 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
                iv.imageTintList = ColorStateList.valueOf(if (esActiva) colorSelectedText else colorUnselectedText)
                tv.setTextColor(if (esActiva) colorSelectedText else colorUnselectedText)
                tv.setTypeface(null, if (esActiva) Typeface.BOLD else Typeface.NORMAL)
            }

            configurarTarjeta(dialogBinding.cardCatTelecom, dialogBinding.ivCatTelecom, dialogBinding.tvCatTelecom, "Telecom")
            configurarTarjeta(dialogBinding.cardCatVivienda, dialogBinding.ivCatVivienda, dialogBinding.tvCatVivienda, "Vivienda")
            configurarTarjeta(dialogBinding.cardCatServicios, dialogBinding.ivCatServicios, dialogBinding.tvCatServicios, "Servicios")
            configurarTarjeta(dialogBinding.cardCatEducacion, dialogBinding.ivCatEducacion, dialogBinding.tvCatEducacion, "Educación")
            configurarTarjeta(dialogBinding.cardCatStreaming, dialogBinding.ivCatStreaming, dialogBinding.tvCatStreaming, "Streaming")
            configurarTarjeta(dialogBinding.cardCatOtros, dialogBinding.ivCatOtros, dialogBinding.tvCatOtros, "Otros")
        }

        // Listeners de tarjetas de categoría
        dialogBinding.cardCatTelecom.setOnClickListener { actualizarSeleccionCategoria("Telecom") }
        dialogBinding.cardCatVivienda.setOnClickListener { actualizarSeleccionCategoria("Vivienda") }
        dialogBinding.cardCatServicios.setOnClickListener { actualizarSeleccionCategoria("Servicios") }
        dialogBinding.cardCatEducacion.setOnClickListener { actualizarSeleccionCategoria("Educación") }
        dialogBinding.cardCatStreaming.setOnClickListener { actualizarSeleccionCategoria("Streaming") }
        dialogBinding.cardCatOtros.setOnClickListener { actualizarSeleccionCategoria("Otros") }

        // Opciones desplegables
        val periodos = arrayOf("Mensual", "Quincenal (Q1/Q2)", "Bimestral", "Anual")
        val diasPago = arrayOf(
            "Día 01",
            "Día 05 de cada mes",
            "Día 10 de cada mes",
            "Día 15 de cada mes",
            "Día 20 de cada mes",
            "Día 30 (Fin de mes)"
        )

        dialogBinding.autoPeriodo.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, periodos))
        dialogBinding.autoDiaPago.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, diasPago))

        // Si es edición, precargar valores
        if (gastoExistente != null) {
            dialogBinding.tvTituloDialogo.text = "Editar Gasto Fijo"
            dialogBinding.etConcepto.setText(gastoExistente.concepto)
            val montoStr = String.format(Locale.US, "%.2f", gastoExistente.monto)
            dialogBinding.etMonto.setText(montoStr)
            
            // Frecuencia
            val periodoMatch = if (gastoExistente.periodo.contains("Quincenal", true)) "Quincenal (Q1/Q2)" else gastoExistente.periodo
            dialogBinding.autoPeriodo.setText(periodoMatch, false)
            
            // Día de pago
            val diaStr = gastoExistente.diaPago
            val diaMatch = diasPago.find { it.contains(diaStr) } ?: "Día $diaStr de cada mes"
            dialogBinding.autoDiaPago.setText(diaMatch, false)

            actualizarSeleccionCategoria(gastoExistente.categoria)
        } else {
            actualizarSeleccionCategoria("Telecom")
        }

        dialogBinding.btnCerrar.setOnClickListener { bottomSheetDialog.dismiss() }
        dialogBinding.btnCancelar.setOnClickListener { bottomSheetDialog.dismiss() }

        dialogBinding.btnGuardar.setOnClickListener {
            val concepto = dialogBinding.etConcepto.text.toString().trim()
            val rawMonto = dialogBinding.etMonto.text.toString()
                .replace("$", "")
                .replace(".", "")
                .replace(",", ".")
                .trim()
            val monto = rawMonto.toDoubleOrNull() ?: 0.0

            val rawDiaPago = dialogBinding.autoDiaPago.text.toString().trim()
            // Extraer el número del día (ej. "Día 05 de cada mes" -> "05")
            val diaPago = Regex("\\d+").find(rawDiaPago)?.value ?: rawDiaPago.ifEmpty { "05" }

            val rawPeriodo = dialogBinding.autoPeriodo.text.toString().trim()
            val periodo = if (rawPeriodo.contains("Quincenal", true)) "Quincenal" else rawPeriodo.ifEmpty { "Mensual" }
            val categoria = categoriaSeleccionada

            if (concepto.isNotEmpty() && monto > 0) {
                if (gastoExistente == null) {
                    db.insertGastoFijo(GastoFijo("", concepto, categoria, monto, diaPago, periodo))
                } else {
                    gastoExistente.concepto = concepto
                    gastoExistente.monto = monto
                    gastoExistente.diaPago = diaPago
                    gastoExistente.categoria = categoria
                    gastoExistente.periodo = periodo
                    db.updateGastoFijo(gastoExistente)
                }
                cargarGastosFijos()
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(mContext, "Por favor complete el concepto y un monto válido", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.show()
    }

    private fun cargarGastosFijos() {
        listaGastosTodos = db.getAllGastosFijos().toMutableList()
        aplicarFiltro()
        actualizarResumenVisual()
    }

    private fun actualizarResumenVisual() {
        if (!isAdded || _binding == null) return

        val total = listaGastosTodos.sumOf { it.monto }
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        val cal = Calendar.getInstance()
        val hoyDia = cal.get(Calendar.DAY_OF_MONTH)
        val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

        var totalPagado = 0.0
        for (g in listaGastosTodos) {
            if (g.periodo.contains("Quincenal", true)) {
                if (g.ultimoPagoQ1 == mesAnioActual) totalPagado += (g.monto / 2)
                if (g.ultimoPagoQ2 == mesAnioActual) totalPagado += (g.monto / 2)
            } else {
                if (g.ultimoPagoQ1 == mesAnioActual) totalPagado += g.monto
            }
        }

        val totalPendiente = (total - totalPagado).coerceAtLeast(0.0)
        val progreso = if (total > 0) ((totalPagado / total) * 100).toInt() else 0

        binding.tvSubStatusGf.text = "${listaGastosTodos.size} compromisos recurrentes"
        binding.chipTodos.text = "Todos (${listaGastosTodos.size})"
        binding.chipServicios.text = "Servicios (${listaGastosTodos.count { it.categoria.contains("serv", true) || it.categoria.contains("luz", true) || it.categoria.contains("agua", true) }})"
        binding.chipVivienda.text = "Vivienda (${listaGastosTodos.count { it.categoria.contains("viv", true) || it.categoria.contains("arriend", true) || it.categoria.contains("casa", true) }})"
        binding.chipTelecom.text = "Telecom (${listaGastosTodos.count { it.categoria.contains("tele", true) || it.categoria.contains("inter", true) || it.categoria.contains("fibra", true) || it.categoria.contains("wifi", true) }})"
        binding.chipSuscripciones.text = "Streaming (${listaGastosTodos.count { it.categoria.contains("stream", true) || it.categoria.contains("suscrip", true) }})"
        binding.chipEducacion.text = "Educación (${listaGastosTodos.count { it.categoria.contains("educ", true) || it.categoria.contains("univ", true) }})"
        binding.chipOtros.text = "Otros (${listaGastosTodos.count { !it.categoria.contains("serv", true) && !it.categoria.contains("viv", true) && !it.categoria.contains("tele", true) && !it.categoria.contains("stream", true) && !it.categoria.contains("educ", true) }})"

        if (esOculto) {
            binding.tvTotalMonto.text = "$ ••••••••"
            binding.tvMontoPagado.text = "$ ••••••••"
            binding.tvMontoPendiente.text = "$ ••••••••"
            binding.btnPrivacyToggleGf.setImageResource(R.drawable.ic_visibility_off)
        } else {
            binding.tvTotalMonto.text = format.format(total)
            binding.tvMontoPagado.text = format.format(totalPagado)
            binding.tvMontoPendiente.text = format.format(totalPendiente)
            binding.btnPrivacyToggleGf.setImageResource(R.drawable.ic_visibility)
        }

        binding.pbLiquidacionGf.progress = progreso
        binding.tvProgresoPorcentaje.text = "$progreso% completado"

        // Cálculo del próximo vencimiento más cercano
        val proximosDias = listaGastosTodos.mapNotNull { g ->
            val d = Regex("\\d+").find(g.diaPago)?.value?.toIntOrNull()
            d?.let { Pair(it, g) }
        }.sortedBy { it.first }

        val proximo = proximosDias.firstOrNull { it.first >= hoyDia } ?: proximosDias.firstOrNull()
        if (proximo != null) {
            val diasRestantes = if (proximo.first >= hoyDia) {
                proximo.first - hoyDia
            } else {
                (30 - hoyDia) + proximo.first
            }
            val diasTexto = when (diasRestantes) {
                0 -> "Hoy"
                1 -> "Mañana"
                else -> "En $diasRestantes días"
            }
            binding.tvProximoVencimientoDetalle.text = "Día ${proximo.first} · $diasTexto"
        } else {
            binding.tvProximoVencimientoDetalle.text = "Sin vencimientos pendientes"
        }

        // Previsión inteligente
        binding.tvPrevisionSubtexto.text = "Tus gastos fijos representan el 42% del presupuesto proyectado para este mes."
    }

    private fun eliminarGastoFijo(gasto: GastoFijo) {
        db.deleteGastoFijo(gasto.id)
        cargarGastosFijos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
