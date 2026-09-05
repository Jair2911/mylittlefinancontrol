package com.example.mylittlefinancontrol.Fragmentos

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylittlefinancontrol.Adaptadores.AhorrosAdapter
import com.example.mylittlefinancontrol.Modelos.Ahorro
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.DialogAbonarAhorroBinding
import com.example.mylittlefinancontrol.databinding.DialogAgregarAhorroBinding
import com.example.mylittlefinancontrol.databinding.FragmentAhorrosBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FragmentAhorros : Fragment() {

    private var _binding: FragmentAhorrosBinding? = null
    private val binding get() = _binding!!

    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var adapter: AhorrosAdapter
    private var listaAhorrosTodos = mutableListOf<Ahorro>()
    private var categoriaFiltroSeleccionada = "Todos"
    private var esOculto = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAhorrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var filtroEstadoAhorros: String = "TODOS"
    private var criterioOrdenAhorros: String = "DEFAULT"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

        setupRecyclerView()
        setupFiltros()
        cargarAhorros()

        binding.tvNuevaMetaBtn.setOnClickListener {
            mostrarDialogoAhorro(null)
        }

        binding.fabAgregarAhorro.setOnClickListener {
            mostrarDialogoAhorro(null)
        }

        binding.btnPrivacyToggleAhorros.setOnClickListener {
            esOculto = !esOculto
            actualizarResumenVisual()
        }

        binding.btnFilterAhorros.setOnClickListener {
            mostrarDialogoFiltroAhorros()
        }
    }

    private fun mostrarDialogoFiltroAhorros() {
        val opciones = arrayOf(
            "Todas las metas (Por defecto)",
            "En progreso (< 100%)",
            "Metas cumplidas (100%)",
            "Ordenar: Mayor % de avance",
            "Ordenar: Menor % de avance",
            "Ordenar: Mayor meta de ahorro ($$$)",
            "Ordenar: Menor meta de ahorro ($)",
            "Ordenar: Alfabéticamente (A-Z)"
        )

        AlertDialog.Builder(mContext)
            .setTitle("Filtrar y Ordenar Metas")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> { filtroEstadoAhorros = "TODOS"; criterioOrdenAhorros = "DEFAULT" }
                    1 -> filtroEstadoAhorros = "EN_PROGRESO"
                    2 -> filtroEstadoAhorros = "COMPLETADAS"
                    3 -> criterioOrdenAhorros = "MAYOR_PROGRESO"
                    4 -> criterioOrdenAhorros = "MENOR_PROGRESO"
                    5 -> criterioOrdenAhorros = "MAYOR_MONTO"
                    6 -> criterioOrdenAhorros = "MENOR_MONTO"
                    7 -> criterioOrdenAhorros = "ALFABETICO"
                }
                aplicarFiltro()
                Toast.makeText(mContext, "Filtro aplicado: ${opciones[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = AhorrosAdapter(
            emptyList(),
            onEditarClick = { ahorro -> mostrarDialogoAhorro(ahorro) },
            onEliminarClick = { ahorro -> eliminarAhorro(ahorro) },
            onAbonarClick = { ahorro -> mostrarDialogoAbono(ahorro) }
        )
        binding.rvAhorros.apply {
            layoutManager = LinearLayoutManager(mContext)
            adapter = this@FragmentAhorros.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFiltros() {
        binding.cgCategoriasAhorros.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedId = checkedIds[0]
            categoriaFiltroSeleccionada = when (selectedId) {
                R.id.chip_ahorro_emergencia -> "Emergencia"
                R.id.chip_ahorro_vehiculo -> "Vehículo"
                R.id.chip_ahorro_viajes -> "Viajes"
                R.id.chip_ahorro_vivienda -> "Vivienda"
                R.id.chip_ahorro_otros -> "Otros"
                else -> "Todos"
            }
            aplicarFiltro()
        }
    }

    private fun aplicarFiltro() {
        var filtrados = when (categoriaFiltroSeleccionada) {
            "Todos" -> listaAhorrosTodos
            "Emergencia" -> listaAhorrosTodos.filter { it.categoria.contains("emerg", true) || it.categoria.contains("salud", true) }
            "Vehículo" -> listaAhorrosTodos.filter { it.categoria.contains("veh", true) || it.categoria.contains("auto", true) || it.categoria.contains("carro", true) }
            "Viajes" -> listaAhorrosTodos.filter { it.categoria.contains("viaj", true) || it.categoria.contains("vaca", true) }
            "Vivienda" -> listaAhorrosTodos.filter { it.categoria.contains("viv", true) || it.categoria.contains("casa", true) || it.categoria.contains("hogar", true) }
            "Otros" -> listaAhorrosTodos.filter {
                !it.categoria.contains("emerg", true) &&
                !it.categoria.contains("veh", true) &&
                !it.categoria.contains("viaj", true) &&
                !it.categoria.contains("viv", true)
            }
            else -> listaAhorrosTodos
        }

        filtrados = when (filtroEstadoAhorros) {
            "EN_PROGRESO" -> filtrados.filter { it.montoActual < it.montoObjetivo }
            "COMPLETADAS" -> filtrados.filter { it.montoActual >= it.montoObjetivo }
            else -> filtrados
        }

        filtrados = when (criterioOrdenAhorros) {
            "MAYOR_PROGRESO" -> filtrados.sortedByDescending { if (it.montoObjetivo > 0) it.montoActual / it.montoObjetivo else 0.0 }
            "MENOR_PROGRESO" -> filtrados.sortedBy { if (it.montoObjetivo > 0) it.montoActual / it.montoObjetivo else 0.0 }
            "MAYOR_MONTO" -> filtrados.sortedByDescending { it.montoObjetivo }
            "MENOR_MONTO" -> filtrados.sortedBy { it.montoObjetivo }
            "ALFABETICO" -> filtrados.sortedBy { it.titulo }
            else -> filtrados
        }

        adapter.actualizarLista(filtrados)
        binding.llEmptyStateAhorros.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun mostrarDialogoAhorro(ahorroExistente: Ahorro?) {
        val bottomSheetDialog = BottomSheetDialog(mContext, R.style.BottomSheetDialogTheme)
        val dialogBinding = DialogAgregarAhorroBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(dialogBinding.root)

        var categoriaSeleccionada = "Emergencia"

        fun actualizarSeleccionCategoria(categoria: String) {
            categoriaSeleccionada = categoria

            val colorSelectedBg = ContextCompat.getColor(mContext, R.color.surface_container_low)
            val colorUnselectedBg = ContextCompat.getColor(mContext, R.color.surface_bg)
            val colorSelectedStroke = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val colorUnselectedStroke = ContextCompat.getColor(mContext, R.color.outline_variant)
            val colorSelectedText = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val colorUnselectedText = ContextCompat.getColor(mContext, R.color.text_secondary)

            fun configurarTarjeta(card: MaterialCardView, iv: ImageView, tv: TextView, nombre: String) {
                val esActiva = categoria.equals(nombre, ignoreCase = true)
                card.setCardBackgroundColor(if (esActiva) colorSelectedBg else colorUnselectedBg)
                card.strokeColor = if (esActiva) colorSelectedStroke else colorUnselectedStroke
                card.strokeWidth = if (esActiva) (1.5 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
                iv.imageTintList = ColorStateList.valueOf(if (esActiva) colorSelectedText else colorUnselectedText)
                tv.setTextColor(if (esActiva) colorSelectedText else colorUnselectedText)
                tv.setTypeface(null, if (esActiva) Typeface.BOLD else Typeface.NORMAL)
            }

            configurarTarjeta(dialogBinding.cardCatEmergencia, dialogBinding.ivCatEmergencia, dialogBinding.tvCatEmergencia, "Emergencia")
            configurarTarjeta(dialogBinding.cardCatVehiculo, dialogBinding.ivCatVehiculo, dialogBinding.tvCatVehiculo, "Vehículo")
            configurarTarjeta(dialogBinding.cardCatViajes, dialogBinding.ivCatViajes, dialogBinding.tvCatViajes, "Viajes")
            configurarTarjeta(dialogBinding.cardCatViviendaAhorro, dialogBinding.ivCatViviendaAhorro, dialogBinding.tvCatViviendaAhorro, "Vivienda")
            configurarTarjeta(dialogBinding.cardCatTecnologia, dialogBinding.ivCatTecnologia, dialogBinding.tvCatTecnologia, "Tecnología")
            configurarTarjeta(dialogBinding.cardCatOtroAhorro, dialogBinding.ivCatOtroAhorro, dialogBinding.tvCatOtroAhorro, "Otro")
        }

        // Listeners de categorías
        dialogBinding.cardCatEmergencia.setOnClickListener { actualizarSeleccionCategoria("Emergencia") }
        dialogBinding.cardCatVehiculo.setOnClickListener { actualizarSeleccionCategoria("Vehículo") }
        dialogBinding.cardCatViajes.setOnClickListener { actualizarSeleccionCategoria("Viajes") }
        dialogBinding.cardCatViviendaAhorro.setOnClickListener { actualizarSeleccionCategoria("Vivienda") }
        dialogBinding.cardCatTecnologia.setOnClickListener { actualizarSeleccionCategoria("Tecnología") }
        dialogBinding.cardCatOtroAhorro.setOnClickListener { actualizarSeleccionCategoria("Otro") }

        // DatePicker para fecha meta
        dialogBinding.etFechaMeta.setOnClickListener {
            val cal = Calendar.getInstance()
            val dp = DatePickerDialog(mContext, { _, year, month, dayOfMonth ->
                val calendarSelected = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val sdf = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                dialogBinding.etFechaMeta.setText(sdf.format(calendarSelected.time).replaceFirstChar { it.uppercase() })
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dp.show()
        }

        if (ahorroExistente != null) {
            dialogBinding.tvTituloDialogo.text = "Editar Meta de Ahorro"
            dialogBinding.etTituloAhorro.setText(ahorroExistente.titulo)
            dialogBinding.etMontoObjetivo.setText(String.format(Locale.US, "%.2f", ahorroExistente.montoObjetivo))
            dialogBinding.etMontoActual.setText(String.format(Locale.US, "%.2f", ahorroExistente.montoActual))
            dialogBinding.etFechaMeta.setText(ahorroExistente.fechaMeta)
            actualizarSeleccionCategoria(ahorroExistente.categoria)
        } else {
            actualizarSeleccionCategoria("Emergencia")
        }

        dialogBinding.btnCerrar.setOnClickListener { bottomSheetDialog.dismiss() }
        dialogBinding.btnCancelar.setOnClickListener { bottomSheetDialog.dismiss() }

        dialogBinding.btnGuardar.setOnClickListener {
            val titulo = dialogBinding.etTituloAhorro.text.toString().trim()
            val rawObjetivo = dialogBinding.etMontoObjetivo.text.toString()
                .replace("$", "").replace(".", "").replace(",", ".").trim()
            val montoObjetivo = rawObjetivo.toDoubleOrNull() ?: 0.0

            val rawActual = dialogBinding.etMontoActual.text.toString()
                .replace("$", "").replace(".", "").replace(",", ".").trim()
            val montoActual = rawActual.toDoubleOrNull() ?: 0.0

            val fechaMeta = dialogBinding.etFechaMeta.text.toString().trim()
            val categoria = categoriaSeleccionada

            if (titulo.isNotEmpty() && montoObjetivo > 0) {
                if (ahorroExistente == null) {
                    db.insertAhorro(Ahorro("", titulo, categoria, montoActual, montoObjetivo, fechaMeta))
                } else {
                    ahorroExistente.titulo = titulo
                    ahorroExistente.montoObjetivo = montoObjetivo
                    ahorroExistente.montoActual = montoActual
                    ahorroExistente.fechaMeta = fechaMeta
                    ahorroExistente.categoria = categoria
                    db.updateAhorro(ahorroExistente)
                }
                cargarAhorros()
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(mContext, "Por favor ingrese el nombre de la meta y un monto objetivo válido", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.show()
    }

    private fun mostrarDialogoAbono(ahorro: Ahorro) {
        val bottomSheetDialog = BottomSheetDialog(mContext, R.style.BottomSheetDialogTheme)
        val dialogBinding = DialogAbonarAhorroBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(dialogBinding.root)

        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        dialogBinding.tvSubtituloMetaAbono.text = ahorro.titulo
        dialogBinding.tvEstadoActualAbono.text = "Progreso actual: ${format.format(ahorro.montoActual)} de ${format.format(ahorro.montoObjetivo)}"

        dialogBinding.btnCerrarAbono.setOnClickListener { bottomSheetDialog.dismiss() }
        dialogBinding.btnCancelarAbono.setOnClickListener { bottomSheetDialog.dismiss() }

        dialogBinding.btnConfirmarAbono.setOnClickListener {
            val rawAbono = dialogBinding.etMontoAbono.text.toString()
                .replace("$", "").replace(".", "").replace(",", ".").trim()
            val abono = rawAbono.toDoubleOrNull() ?: 0.0

            if (abono > 0) {
                ahorro.montoActual += abono
                db.updateAhorro(ahorro)
                cargarAhorros()
                bottomSheetDialog.dismiss()
                Toast.makeText(mContext, "Aporte de ${format.format(abono)} registrado con éxito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(mContext, "Por favor ingrese un monto de aporte válido", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.show()
    }

    override fun onResume() {
        super.onResume()
        cargarAhorros()
    }

    private fun cargarAhorros() {
        listaAhorrosTodos = db.getAllAhorros().toMutableList()
        aplicarFiltro()
        actualizarResumenVisual()
    }

    private fun actualizarResumenVisual() {
        if (!isAdded || _binding == null) return

        val totalAhorrado = listaAhorrosTodos.sumOf { it.montoActual }
        val totalMetaGlobal = listaAhorrosTodos.sumOf { it.montoObjetivo }
        val faltante = (totalMetaGlobal - totalAhorrado).coerceAtLeast(0.0)
        val progresoGlobal = if (totalMetaGlobal > 0) ((totalAhorrado / totalMetaGlobal) * 100).toInt() else 0
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        binding.tvSubStatusAhorros.text = "${listaAhorrosTodos.size} metas de ahorro activas"
        binding.tvBadgeMetasActivas.text = "${listaAhorrosTodos.size} Metas Activas"

        if (esOculto) {
            binding.tvTotalAhorradoMonto.text = "$ ••••••••"
            binding.tvMetaGlobalMonto.text = "Meta Global: $ ••••••••"
            binding.tvMetaFaltanteMonto.text = "Faltan: $ ••••••••"
            binding.btnPrivacyToggleAhorros.setImageResource(R.drawable.ic_visibility_off)
        } else {
            binding.tvTotalAhorradoMonto.text = format.format(totalAhorrado)
            binding.tvMetaGlobalMonto.text = "Meta Global: ${format.format(totalMetaGlobal)}"
            binding.tvMetaFaltanteMonto.text = "Faltan: ${format.format(faltante)}"
            binding.btnPrivacyToggleAhorros.setImageResource(R.drawable.ic_visibility)
        }

        binding.pbMetaGlobal.progress = progresoGlobal
        binding.tvProgresoGlobalPorcentaje.text = "$progresoGlobal% Completado"

        // Actualizar conteos de chips
        binding.chipAhorroTodos.text = "Todos (${listaAhorrosTodos.size})"
        binding.chipAhorroEmergencia.text = "Emergencia (${listaAhorrosTodos.count { it.categoria.contains("emerg", true) || it.categoria.contains("salud", true) }})"
        binding.chipAhorroVehiculo.text = "Vehículo (${listaAhorrosTodos.count { it.categoria.contains("veh", true) || it.categoria.contains("auto", true) || it.categoria.contains("carro", true) }})"
        binding.chipAhorroViajes.text = "Viajes (${listaAhorrosTodos.count { it.categoria.contains("viaj", true) || it.categoria.contains("vaca", true) }})"
        binding.chipAhorroVivienda.text = "Vivienda (${listaAhorrosTodos.count { it.categoria.contains("viv", true) || it.categoria.contains("casa", true) || it.categoria.contains("hogar", true) }})"
        binding.chipAhorroOtros.text = "Otros (${listaAhorrosTodos.count { !it.categoria.contains("emerg", true) && !it.categoria.contains("veh", true) && !it.categoria.contains("viaj", true) && !it.categoria.contains("viv", true) }})"
    }

    private fun eliminarAhorro(ahorro: Ahorro) {
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Meta de Ahorro")
            .setMessage("¿Estás seguro de que deseas eliminar '${ahorro.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteAhorro(ahorro.id)
                cargarAhorros()
                Toast.makeText(mContext, "Meta eliminada correctamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}