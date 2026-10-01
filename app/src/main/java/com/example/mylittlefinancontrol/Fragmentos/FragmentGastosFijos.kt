package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mylittlefinancontrol.Adaptadores.GastosFijosAdapter
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.Servicios.GastoFijoWorker
import com.example.mylittlefinancontrol.databinding.DialogAgregarGastoFijoBinding
import com.example.mylittlefinancontrol.databinding.DialogControlAnualGastoBinding
import com.example.mylittlefinancontrol.databinding.FragmentGastosFijosBinding
import com.example.mylittlefinancontrol.databinding.ItemMesControlAnualBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

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

    private var filtroEstado: String = "TODOS"
    private var criterioOrdenGf: String = "DEFAULT"

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
            mostrarResumenCompromisosMes()
        }

        binding.btnFilterGf.setOnClickListener {
            mostrarDialogoFiltroGastosFijos()
        }
    }

    private fun mostrarResumenCompromisosMes() {
        val cal = Calendar.getInstance()
        val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val nombresMeses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        val nombreMes = nombresMeses[cal.get(Calendar.MONTH)]

        val pagados = listaGastosTodos.count { g ->
            if (g.periodo == "Quincenal") g.ultimoPagoQ1 == mesAnioActual && g.ultimoPagoQ2 == mesAnioActual else g.ultimoPagoQ1 == mesAnioActual
        }
        val pendientes = listaGastosTodos.size - pagados
        val totalMonto = listaGastosTodos.sumOf { it.monto }

        AlertDialog.Builder(mContext)
            .setTitle("Compromisos de $nombreMes ${cal.get(Calendar.YEAR)}")
            .setMessage("• Total compromisos: ${listaGastosTodos.size}\n• Pagados este mes: $pagados\n• Pendientes: $pendientes\n• Presupuesto fijo total: ${java.text.NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(totalMonto)}")
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun mostrarDialogoFiltroGastosFijos() {
        val opciones = arrayOf(
            "Todos los compromisos (Por defecto)",
            "Solo pendientes de pago",
            "Solo pagados este mes",
            "Solo frecuencia mensual",
            "Solo frecuencia quincenal",
            "Ordenar: Mayor a menor monto ($$$)",
            "Ordenar: Menor a mayor monto ($)",
            "Ordenar: Alfabéticamente (A-Z)"
        )

        AlertDialog.Builder(mContext)
            .setTitle("Filtrar y Ordenar Gastos Fijos")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> { filtroEstado = "TODOS"; criterioOrdenGf = "DEFAULT" }
                    1 -> filtroEstado = "PENDIENTES"
                    2 -> filtroEstado = "PAGADOS"
                    3 -> filtroEstado = "MENSUAL"
                    4 -> filtroEstado = "QUINCENAL"
                    5 -> criterioOrdenGf = "MAYOR_MONTO"
                    6 -> criterioOrdenGf = "MENOR_MONTO"
                    7 -> criterioOrdenGf = "ALFABETICO"
                }
                aplicarFiltro()
                Toast.makeText(mContext, "Filtro aplicado: ${opciones[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        cargarGastosFijos()
    }

    private fun setupRecyclerView() {
        adapter = GastosFijosAdapter(
            emptyList(),
            onEditarClick = { gasto -> mostrarDialogoGastoFijo(gasto) },
            onEliminarClick = { gasto -> eliminarGastoFijo(gasto) },
            onControlAnualClick = { gasto -> mostrarDialogoControlAnual(gasto) }
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
        var filtrados = when (categoriaFiltroSeleccionada) {
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

        val cal = Calendar.getInstance()
        val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

        fun estaPagadoTotal(g: GastoFijo): Boolean {
            return if (g.periodo == "Quincenal") {
                g.ultimoPagoQ1 == mesAnioActual && g.ultimoPagoQ2 == mesAnioActual
            } else {
                g.ultimoPagoQ1 == mesAnioActual
            }
        }

        filtrados = when (filtroEstado) {
            "PENDIENTES" -> filtrados.filter { !estaPagadoTotal(it) }
            "PAGADOS" -> filtrados.filter { estaPagadoTotal(it) }
            "MENSUAL" -> filtrados.filter { it.periodo.equals("Mensual", ignoreCase = true) }
            "QUINCENAL" -> filtrados.filter { it.periodo.equals("Quincenal", ignoreCase = true) }
            else -> filtrados
        }

        filtrados = when (criterioOrdenGf) {
            "MAYOR_MONTO" -> filtrados.sortedByDescending { it.monto }
            "MENOR_MONTO" -> filtrados.sortedBy { it.monto }
            "ALFABETICO" -> filtrados.sortedBy { it.concepto }
            else -> filtrados
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

            dialogBinding.swRecordatorio.isChecked = gastoExistente.recordatorio
            actualizarSeleccionCategoria(gastoExistente.categoria)
        } else {
            dialogBinding.swRecordatorio.isChecked = true
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
            val recordatorio = dialogBinding.swRecordatorio.isChecked

            if (concepto.isNotEmpty() && monto > 0) {
                if (gastoExistente == null) {
                    db.insertGastoFijo(GastoFijo("", concepto, categoria, monto, diaPago, periodo, recordatorio = recordatorio))
                } else {
                    gastoExistente.concepto = concepto
                    gastoExistente.monto = monto
                    gastoExistente.diaPago = diaPago
                    gastoExistente.categoria = categoria
                    gastoExistente.periodo = periodo
                    gastoExistente.recordatorio = recordatorio
                    db.updateGastoFijo(gastoExistente)
                }
                programarVerificacionGastoFijoWorker(mContext)
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

    private fun mostrarDialogoControlAnual(gasto: GastoFijo) {
        val bottomSheetDialog = BottomSheetDialog(mContext, R.style.BottomSheetDialogTheme)
        val dialogBinding = DialogControlAnualGastoBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(dialogBinding.root)

        val calActual = Calendar.getInstance()
        val anioActual = calActual.get(Calendar.YEAR)
        val mesActual = calActual.get(Calendar.MONTH) // 0 = Enero

        dialogBinding.tvTituloControlAnual.text = "📅 Control Anual $anioActual"
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        dialogBinding.tvSubtituloGasto.text = "${gasto.concepto} · ${format.format(gasto.monto)} / mes"

        dialogBinding.btnCerrarControl.setOnClickListener { bottomSheetDialog.dismiss() }

        // Consultar movimientos para verificar qué meses se han pagado
        val todosMovimientos = db.getAllNotas()
        val gfIdInt = gasto.id.toIntOrNull() ?: -1

        val nombresMeses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        class MesEstadoItem(
            val numeroMes: Int, // 1 a 12
            val nombreMes: String,
            val estaPagado: Boolean,
            val fechaPagoTexto: String
        )

        val listaMeses = mutableListOf<MesEstadoItem>()

        for (m in 1..12) {
            val mesClave = String.format(Locale.US, "%04d-%02d", anioActual, m)
            
            // Buscar si hay movimiento para este gasto fijo en este mes
            val movEncontrado = todosMovimientos.firstOrNull { mov ->
                val idCoincide = (mov.gastoFijoId == gfIdInt && gfIdInt != -1) ||
                        (mov.descripcion.contains(gasto.concepto, ignoreCase = true) && mov.tipo_reg.contains("GASTO", true))
                val fechaEfectiva = if (mov.fecha.isNotBlank()) mov.fecha else mov.fechaCreacion
                val fechaCoincide = fechaEfectiva.startsWith(mesClave)
                idCoincide && fechaCoincide
            }

            var pagado = movEncontrado != null
            if (!pagado && m == (mesActual + 1)) {
                pagado = gasto.ultimoPagoQ1 == mesClave
            }

            val detalle = if (pagado) {
                if (movEncontrado != null && movEncontrado.fecha.isNotBlank()) {
                    "Pagado el ${movEncontrado.fecha.split(" ")[0]}"
                } else {
                    "Pagado en $mesClave"
                }
            } else {
                if (m <= (mesActual + 1)) "⏱️ Pendiente de pago" else "Próximo pago"
            }

            listaMeses.add(MesEstadoItem(m, "${nombresMeses[m - 1]} $anioActual", pagado, detalle))
        }

        // Calcular resumen general
        val mesesTranscurridos = (mesActual + 1)
        val mesesPagadosHastaHoy = listaMeses.take(mesesTranscurridos).count { it.estaPagado }
        val mesesPendientes = mesesTranscurridos - mesesPagadosHastaHoy

        if (mesesPendientes <= 0) {
            dialogBinding.tvTextoResumenAnual.text = "🟢 ¡Vas 100% al día con tus pagos de $anioActual!"
            dialogBinding.ivIconoResumenAnual.setImageResource(R.drawable.ic_check_circle)
            dialogBinding.ivIconoResumenAnual.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.primary_emerald))
        } else {
            dialogBinding.tvTextoResumenAnual.text = "🔴 Tienes $mesesPendientes mes(es) pendiente(s) de pago este año"
            dialogBinding.ivIconoResumenAnual.setImageResource(R.drawable.ic_local_fire_department)
            dialogBinding.ivIconoResumenAnual.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.expense_red))
        }

        // Adapter para los 12 meses
        dialogBinding.rvMesesControlAnual.layoutManager = LinearLayoutManager(mContext)
        val adapterMeses = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val b = ItemMesControlAnualBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return object : RecyclerView.ViewHolder(b.root) {}
            }

            override fun getItemCount() = listaMeses.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = listaMeses[position]
                val b = ItemMesControlAnualBinding.bind(holder.itemView)

                b.tvNombreMes.text = item.nombreMes
                b.tvDetallePagoMes.text = item.fechaPagoTexto

                if (item.estaPagado) {
                    b.tvBadgeEstadoMes.visibility = View.VISIBLE
                    b.tvBadgeEstadoMes.text = "✓ PAGADO"
                    b.tvBadgeEstadoMes.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.primary_fixed))
                    b.tvBadgeEstadoMes.setTextColor(ContextCompat.getColor(mContext, R.color.on_primary_fixed))
                    b.btnPagarMes.visibility = View.GONE
                } else {
                    b.tvBadgeEstadoMes.visibility = View.GONE
                    b.btnPagarMes.visibility = View.VISIBLE
                    b.btnPagarMes.text = "Pagar ${format.format(gasto.monto)}"

                    b.btnPagarMes.setOnClickListener {
                        val mesFormatted = String.format(Locale.US, "%02d", item.numeroMes)
                        val mesClave = "$anioActual-$mesFormatted"
                        val diaNum = gasto.diaPago.trim().filter { it.isDigit() }.toIntOrNull() ?: 15
                        val fechaPago = "$anioActual-$mesFormatted-${String.format(Locale.US, "%02d", diaNum)} 12:00:00"

                        gasto.ultimoPagoQ1 = mesClave
                        db.updateGastoFijo(gasto)

                        val nuevoMov = Movimiento(
                            id = 0,
                            descripcion = "PAGO: ${gasto.concepto}".uppercase(),
                            valor = gasto.monto.toInt(),
                            tipo_reg = "GASTO FIJO",
                            fecha = fechaPago,
                            gastoFijoId = gfIdInt,
                            quincenaPagada = 0,
                            categoria = gasto.categoria,
                            fechaCreacion = Constantes.formatoFecha(System.currentTimeMillis())
                        )
                        db.insertNota(nuevoMov)

                        Toast.makeText(mContext, "Pago de ${nombresMeses[item.numeroMes - 1]} registrado", Toast.LENGTH_SHORT).show()
                        bottomSheetDialog.dismiss()
                        cargarGastosFijos()
                    }
                }
            }
        }

        dialogBinding.rvMesesControlAnual.adapter = adapterMeses
        bottomSheetDialog.show()
    }

    private fun programarVerificacionGastoFijoWorker(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            val periodicWorkRequest = PeriodicWorkRequestBuilder<GastoFijoWorker>(
                24, TimeUnit.HOURS
            ).build()

            workManager.enqueueUniquePeriodicWork(
                "GastoFijoWorkerPeriodic",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )

            val oneTimeRequest = OneTimeWorkRequestBuilder<GastoFijoWorker>().build()
            workManager.enqueue(oneTimeRequest)
        } catch (e: Exception) {
            Log.e("FragmentGastosFijos", "Error programando GastoFijoWorker: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
