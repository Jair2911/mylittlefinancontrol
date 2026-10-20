package com.example.mylittlefinancontrol.Fragmentos

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylittlefinancontrol.BuildConfig
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasAdaptador
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.DialogAgregarMovimientoBinding
import com.example.mylittlefinancontrol.databinding.FragmentMovimientoBinding
import com.example.mylittlefinancontrol.Servicios.DatosTransaccion
import com.example.mylittlefinancontrol.Servicios.TransaccionBancariaParser
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FragmentMovimiento : Fragment() {

    private lateinit var binding: FragmentMovimientoBinding
    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var notasAdaptador: NotasAdaptador

    private var mesSeleccionado: Int = 0
    private var anioSeleccionado: Int = 0
    private val listaDeudas = mutableListOf<Deuda>()
    private var deudaSeleccionada: Deuda? = null
    private val listaGastosFijos = mutableListOf<GastoFijo>()
    private var gastoFijoSeleccionado: GastoFijo? = null

    // ML Kit y Cámara
    private var currentDialogBinding: DialogAgregarMovimientoBinding? = null
    private var photoUri: Uri? = null
    private var onDatosEscaneadosListener: ((DatosTransaccion) -> Unit)? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { procesarImagenConMLKit(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { procesarImagenConMLKit(it) }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            abrirCamara()
        } else {
            Toast.makeText(mContext, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMovimientoBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var categoriaFiltro: String = "TODOS"
    private var searchQuery: String = ""
    private var esSaldoOculto: Boolean = false
    private var saldoRealTexto: String = "$ 0,00"
    private var criterioOrden: String = "RECIENTES"

    private var cantidadAMostrar: Int = 10
    private val PASO_PAGINACION: Int = 10
    private var listaTotalFiltrada: List<Movimiento> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper.getInstance(mContext)

        setupFiltros()
        setupRecyclerView()
        setupPaginacionScroll()
        cargarDeudasLocales()

        binding.FABAgregarNota.setOnClickListener {
            mostrarDialogoMovimiento(null)
        }

        binding.ivFiltroAvanzado.setOnClickListener {
            mostrarDialogoFiltroAvanzado()
        }
    }

    private fun setupPaginacionScroll() {
        binding.btnCargarMas.setOnClickListener {
            cargarSiguientesMovimientos()
        }
    }

    private fun cargarSiguientesMovimientos() {
        if (cantidadAMostrar < listaTotalFiltrada.size) {
            cantidadAMostrar += PASO_PAGINACION
            val sublista = listaTotalFiltrada.take(cantidadAMostrar)
            lifecycleScope.launch(Dispatchers.IO) {
                val displayList = NotasAdaptador.procesarGrupos(sublista)
                withContext(Dispatchers.Main) {
                    notasAdaptador.submitList(displayList)
                    actualizarPaginacionUI(sublista)
                }
            }
        }
    }

    private fun actualizarPaginacionUI(sublista: List<Movimiento>) {
        val cantTotal = listaTotalFiltrada.size
        val cantMostrada = sublista.size
        binding.tvConteoRegistros.text = when {
            cantTotal == 0 -> "0 Registros"
            cantMostrada < cantTotal -> "$cantMostrada de $cantTotal Registros"
            cantTotal == 1 -> "1 Registro"
            else -> "$cantTotal Registros"
        }

        if (sublista.size < listaTotalFiltrada.size) {
            val restantes = listaTotalFiltrada.size - sublista.size
            val proximoPaso = if (restantes > PASO_PAGINACION) PASO_PAGINACION else restantes
            binding.tvBtnCargarMas.text = "Cargar $proximoPaso anteriores (quedan $restantes)"
            binding.llCargarMasMovimientos.visibility = View.VISIBLE
            binding.llFinalListaMovimientos.visibility = View.GONE
        } else {
            binding.llCargarMasMovimientos.visibility = View.GONE
            binding.llFinalListaMovimientos.visibility = if (listaTotalFiltrada.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDeudasLocales()
        actualizarListaFiltrada(resetearPaginacion = false)
    }

    private fun extraerAnioYMes(fechaStr: String): Pair<Int, Int>? {
        if (fechaStr.isBlank()) return null
        val ts = fechaStr.trim().toLongOrNull()
        if (ts != null && ts > 100000000000L) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = ts
            return Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
        val clean = fechaStr.trim().split(" ")[0]
        val delimiters = charArrayOf('-', '/', '.')
        for (d in delimiters) {
            if (clean.contains(d)) {
                val parts = clean.split(d)
                if (parts.size >= 3) {
                    val p0 = parts[0].toIntOrNull()
                    val p1 = parts[1].toIntOrNull()
                    val p2 = parts[2].toIntOrNull()
                    if (p0 != null && p1 != null && p2 != null) {
                        if (p0 > 1000) {
                            // Formato yyyy-MM-dd
                            return Pair(p0, p1 - 1)
                        } else if (p2 > 1000) {
                            // Formato dd-MM-yyyy
                            return Pair(p2, p1 - 1)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun setupFiltros() {
        val calendar = Calendar.getInstance()
        val anioActual = calendar.get(Calendar.YEAR)

        mesSeleccionado = -1
        anioSeleccionado = -1

        val mesesDisplay = arrayOf("Todos los meses", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        binding.tvMesSeleccionado.text = "Todos los meses"
        binding.tvAnioSeleccionado.text = "Todos"

        // Selector de Mes
        binding.btnSelectorMes.setOnClickListener {
            AlertDialog.Builder(mContext)
                .setTitle("Seleccionar Periodo")
                .setItems(mesesDisplay) { _, which ->
                    if (which == 0) {
                        mesSeleccionado = -1
                        binding.tvMesSeleccionado.text = "Todos los meses"
                    } else {
                        mesSeleccionado = which - 1
                        binding.tvMesSeleccionado.text = mesesDisplay[which]
                    }
                    actualizarListaFiltrada()
                }
                .show()
        }

        // Selector de Año
        binding.btnSelectorAnio.setOnClickListener {
            val aniosDisplay = arrayOf("Todos los años", (anioActual - 2).toString(), (anioActual - 1).toString(), anioActual.toString(), (anioActual + 1).toString(), (anioActual + 2).toString())
            AlertDialog.Builder(mContext)
                .setTitle("Seleccionar Año")
                .setItems(aniosDisplay) { _, which ->
                    if (which == 0) {
                        anioSeleccionado = -1
                        binding.tvAnioSeleccionado.text = "Todos"
                    } else {
                        anioSeleccionado = aniosDisplay[which].toInt()
                        binding.tvAnioSeleccionado.text = aniosDisplay[which]
                    }
                    actualizarListaFiltrada()
                }
                .show()
        }

        // Exportar Reporte
        binding.btnExportarReporte.setOnClickListener {
            val mesTxt = if (mesSeleccionado == -1) "Histórico completo" else mesesDisplay[mesSeleccionado + 1]
            val anioTxt = if (anioSeleccionado == -1) "todos los años" else anioSeleccionado.toString()
            Toast.makeText(mContext, "Reporte de $mesTxt $anioTxt exportado", Toast.LENGTH_SHORT).show()
        }

        // Privacidad
        binding.btnTogglePrivacidadMov.setOnClickListener {
            esSaldoOculto = !esSaldoOculto
            if (esSaldoOculto) {
                binding.tvTotalMovMonto.text = "••••••••"
                binding.btnTogglePrivacidadMov.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.tvTotalMovMonto.text = saldoRealTexto
                binding.btnTogglePrivacidadMov.setImageResource(R.drawable.ic_visibility)
            }
        }

        // Buscador en tiempo real
        binding.etBuscarMovimientos.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                actualizarListaFiltrada()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Chips de Categorías
        fun actualizarChipsUI(categoria: String) {
            categoriaFiltro = categoria
            val chipActivoBg = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val chipInactivoBg = ContextCompat.getColor(mContext, R.color.surface_card)
            val chipActivoText = ContextCompat.getColor(mContext, android.R.color.white)
            val chipInactivoText = ContextCompat.getColor(mContext, R.color.text_secondary)

            fun pintarChip(tv: TextView, activo: Boolean) {
                tv.backgroundTintList = ColorStateList.valueOf(if (activo) chipActivoBg else chipInactivoBg)
                tv.setTextColor(if (activo) chipActivoText else chipInactivoText)
            }

            pintarChip(binding.chipFiltroTodos, categoria == "TODOS")
            pintarChip(binding.chipFiltroGastosFijos, categoria == "GASTOS FIJOS")
            pintarChip(binding.chipFiltroSupermercado, categoria == "SUPERMERCADO")
            pintarChip(binding.chipFiltroIngresos, categoria == "INGRESOS")
            pintarChip(binding.chipFiltroDeudas, categoria == "DEUDAS")
            pintarChip(binding.chipFiltroServicios, categoria == "SERVICIOS")
            pintarChip(binding.chipFiltroComida, categoria == "COMIDA")
            pintarChip(binding.chipFiltroTransporte, categoria == "TRANSPORTE")
            pintarChip(binding.chipFiltroSalud, categoria == "SALUD")
            pintarChip(binding.chipFiltroStreaming, categoria == "STREAMING")
            pintarChip(binding.chipFiltroEducacion, categoria == "EDUCACION")
            pintarChip(binding.chipFiltroOtros, categoria == "OTROS")

            actualizarListaFiltrada()
        }

        binding.chipFiltroTodos.setOnClickListener { actualizarChipsUI("TODOS") }
        binding.chipFiltroGastosFijos.setOnClickListener { actualizarChipsUI("GASTOS FIJOS") }
        binding.chipFiltroSupermercado.setOnClickListener { actualizarChipsUI("SUPERMERCADO") }
        binding.chipFiltroIngresos.setOnClickListener { actualizarChipsUI("INGRESOS") }
        binding.chipFiltroDeudas.setOnClickListener { actualizarChipsUI("DEUDAS") }
        binding.chipFiltroServicios.setOnClickListener { actualizarChipsUI("SERVICIOS") }
        binding.chipFiltroComida.setOnClickListener { actualizarChipsUI("COMIDA") }
        binding.chipFiltroTransporte.setOnClickListener { actualizarChipsUI("TRANSPORTE") }
        binding.chipFiltroSalud.setOnClickListener { actualizarChipsUI("SALUD") }
        binding.chipFiltroStreaming.setOnClickListener { actualizarChipsUI("STREAMING") }
        binding.chipFiltroEducacion.setOnClickListener { actualizarChipsUI("EDUCACION") }
        binding.chipFiltroOtros.setOnClickListener { actualizarChipsUI("OTROS") }
    }

    private fun setupRecyclerView() {
        notasAdaptador = NotasAdaptador(
            mContext,
            onEditarClick = { mov -> mostrarDialogoMovimiento(mov) },
            onEliminarClick = { mov -> eliminarMovimiento(mov) }
        )
        binding.notasRv.layoutManager = LinearLayoutManager(mContext)
        binding.notasRv.adapter = notasAdaptador
    }

    private fun evaluarCoincideCategoria(
        mov: Movimiento,
        categoria: String,
        tipoUpper: String = mov.tipo_reg.uppercase(),
        descUpper: String = mov.descripcion.uppercase(),
        catUpper: String = mov.categoria.uppercase()
    ): Boolean {
        if (categoria == "TODOS") return true

        // Coincidencia directa si la categoría fue elegida explícitamente al crear el movimiento
        if (catUpper.isNotBlank() && catUpper != "VARIOS" && catUpper != "SIN CATEGORÍA") {
            if (catUpper.contains(categoria, ignoreCase = true) || categoria.contains(catUpper, ignoreCase = true)) {
                return true
            }
            if ((categoria == "STREAMING" || categoria == "OCIO") && (catUpper.contains("PELÍCULA") || catUpper.contains("OCIO"))) {
                return true
            }
            if (categoria == "DEUDAS" && catUpper.contains("PRÉSTAMO")) return true
        }

        return when (categoria) {
            "TODOS" -> true
            "GASTOS FIJOS" -> tipoUpper.contains("GASTO FIJO") || descUpper.contains("PAGO:") || descUpper.contains("ARRIENDO") || descUpper.contains("FIBRA")
            "SUPERMERCADO" -> catUpper.contains("SUPERMERCADO") || descUpper.contains("SUPERMERCADO") || descUpper.contains("DESPENSA") || descUpper.contains("ÉXITO") || descUpper.contains("CARULLA") || descUpper.contains("D1")
            "INGRESOS" -> tipoUpper.contains("INGRESO") || catUpper.contains("INGRESO") || descUpper.contains("HONORARIOS") || descUpper.contains("SUELDO") || descUpper.contains("NÓMINA") || descUpper.contains("VENTA")
            "DEUDAS" -> tipoUpper.contains("PAGO DEUDA") || tipoUpper.contains("DEUDA") || catUpper.contains("PRÉSTAMO") || descUpper.contains("TARJETA") || descUpper.contains("CRÉDITO") || descUpper.contains("PRÉSTAMO")
            "SERVICIOS" -> catUpper.contains("SERVICIO") || descUpper.contains("SERVICIO") || descUpper.contains("ENERGÍA") || descUpper.contains("AGUA") || descUpper.contains("LUZ") || descUpper.contains("INTERNET") || descUpper.contains("MÓVIL") || descUpper.contains("GAS")
            "COMIDA" -> catUpper.contains("COMIDA") || descUpper.contains("RESTAURANTE") || descUpper.contains("CAFÉ") || descUpper.contains("MERIENDA") || descUpper.contains("COMIDA") || descUpper.contains("ALMUERZO") || descUpper.contains("CENA")
            "TRANSPORTE" -> catUpper.contains("TRANSPORTE") || descUpper.contains("TRANSPORTE") || descUpper.contains("GASOLINA") || descUpper.contains("UBER") || descUpper.contains("TAXI") || descUpper.contains("ESTACIÓN")
            "SALUD" -> catUpper.contains("SALUD") || descUpper.contains("FARMACIA") || descUpper.contains("SALUD") || descUpper.contains("MÉDICO") || descUpper.contains("DROGUERÍA") || descUpper.contains("HOSPITAL")
            "STREAMING" -> catUpper.contains("STREAMING") || catUpper.contains("PELÍCULA") || catUpper.contains("OCIO") || descUpper.contains("NETFLIX") || descUpper.contains("SPOTIFY") || descUpper.contains("DISNEY") || descUpper.contains("PRIME") || descUpper.contains("CINE")
            "EDUCACION" -> catUpper.contains("EDUCACION") || descUpper.contains("ESPECIALIZACIÓN") || descUpper.contains("CURSO") || descUpper.contains("EDUCACIÓN") || descUpper.contains("UNIVERSIDAD") || descUpper.contains("COLEGIO")
            "OTROS" -> true
            else -> false
        }
    }

    private fun actualizarListaFiltrada(resetearPaginacion: Boolean = true) {
        if (resetearPaginacion) {
            cantidadAMostrar = PASO_PAGINACION
        }
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        lifecycleScope.launch(Dispatchers.IO) {
            val todos = db.getAllNotas()

            val baseFechaBusqueda = todos.filter { mov ->
                try {
                    val parsed = extraerAnioYMes(mov.fecha)
                    val coincideFecha = when {
                        mesSeleccionado == -1 && anioSeleccionado == -1 -> true
                        mesSeleccionado == -1 && anioSeleccionado != -1 -> (parsed == null || parsed.first == anioSeleccionado)
                        mesSeleccionado != -1 && anioSeleccionado == -1 -> (parsed != null && parsed.second == mesSeleccionado)
                        else -> (parsed != null && parsed.first == anioSeleccionado && parsed.second == mesSeleccionado)
                    }
                    val coincideBusqueda = searchQuery.isEmpty() || mov.descripcion.contains(searchQuery, ignoreCase = true)
                    coincideFecha && coincideBusqueda
                } catch (e: Exception) { true }
            }

            // Conteo dinámico optimizado
            val categoriasList = arrayOf("GASTOS FIJOS", "SUPERMERCADO", "INGRESOS", "DEUDAS", "SERVICIOS", "COMIDA", "TRANSPORTE", "SALUD", "STREAMING", "EDUCACION", "OTROS")
            val conteosMap = mutableMapOf<String, Int>()

            for (mov in baseFechaBusqueda) {
                val tUpper = mov.tipo_reg.uppercase()
                val dUpper = mov.descripcion.uppercase()
                val cUpper = mov.categoria.uppercase()

                for (cat in categoriasList) {
                    if (evaluarCoincideCategoria(mov, cat, tUpper, dUpper, cUpper)) {
                        conteosMap[cat] = (conteosMap[cat] ?: 0) + 1
                    }
                }
            }

            val filtrados = baseFechaBusqueda.filter { evaluarCoincideCategoria(it, categoriaFiltro) }

            val ordenados = when (criterioOrden) {
                "ANTIGUOS" -> filtrados.reversed()
                "MAYOR_MONTO" -> filtrados.sortedByDescending { it.valor }
                "MENOR_MONTO" -> filtrados.sortedBy { it.valor }
                "ALFABETICO" -> filtrados.sortedBy { it.descripcion }
                else -> filtrados
            }

            val totalIngresos = filtrados.filter { it.tipo_reg.contains("INGRESO", ignoreCase = true) }.sumOf { it.valor.toDouble() }
            val totalEgresos = filtrados.filter { !it.tipo_reg.contains("INGRESO", ignoreCase = true) }.sumOf { it.valor.toDouble() }

            val montoAMostrar = when (categoriaFiltro) {
                "INGRESOS" -> totalIngresos
                "TODOS" -> if (totalEgresos > 0) totalEgresos else totalIngresos
                else -> totalEgresos
            }

            listaTotalFiltrada = ordenados
            val sublista = listaTotalFiltrada.take(cantidadAMostrar)
            val displayList = NotasAdaptador.procesarGrupos(sublista)

            withContext(Dispatchers.Main) {
                fun conteo(cat: String) = conteosMap[cat] ?: 0
                binding.chipFiltroTodos.text = "Todos (${baseFechaBusqueda.size})"
                binding.chipFiltroGastosFijos.text = "Gastos Fijos (${conteo("GASTOS FIJOS")})"
                binding.chipFiltroSupermercado.text = "Supermercado (${conteo("SUPERMERCADO")})"
                binding.chipFiltroIngresos.text = "Ingresos (${conteo("INGRESOS")})"
                binding.chipFiltroDeudas.text = "Deudas (${conteo("DEUDAS")})"
                binding.chipFiltroServicios.text = "Servicios (${conteo("SERVICIOS")})"
                binding.chipFiltroComida.text = "Comida (${conteo("COMIDA")})"
                binding.chipFiltroTransporte.text = "Transporte (${conteo("TRANSPORTE")})"
                binding.chipFiltroSalud.text = "Salud (${conteo("SALUD")})"
                binding.chipFiltroStreaming.text = "Streaming (${conteo("STREAMING")})"
                binding.chipFiltroEducacion.text = "Educación (${conteo("EDUCACION")})"
                binding.chipFiltroOtros.text = "Otros (${conteo("OTROS")})"

                binding.tvTituloBalanceMov.text = when (categoriaFiltro) {
                    "TODOS" -> "TOTAL DE MOVIMIENTOS"
                    "GASTOS FIJOS" -> "TOTAL GASTOS FIJOS"
                    "SUPERMERCADO" -> "TOTAL SUPERMERCADO"
                    "INGRESOS" -> "TOTAL INGRESOS"
                    "DEUDAS" -> "TOTAL PAGOS DEUDAS"
                    "SERVICIOS" -> "TOTAL SERVICIOS"
                    "COMIDA" -> "TOTAL COMIDA Y RESTAURANTES"
                    "TRANSPORTE" -> "TOTAL TRANSPORTE"
                    "SALUD" -> "TOTAL SALUD Y FARMACIA"
                    "STREAMING" -> "TOTAL STREAMING"
                    "EDUCACION" -> "TOTAL EDUCACIÓN"
                    "OTROS" -> "TOTAL OTROS GASTOS"
                    else -> "TOTAL DE MOVIMIENTOS"
                }

                listaTotalFiltrada = ordenados
                val sublista = listaTotalFiltrada.take(cantidadAMostrar)
                notasAdaptador.submitList(displayList)
                actualizarPaginacionUI(sublista)

                saldoRealTexto = format.format(montoAMostrar)
                if (!esSaldoOculto) {
                    binding.tvTotalMovMonto.text = saldoRealTexto
                }

                binding.tvTotalIngresosMonto.text = "+${format.format(totalIngresos)}"
                binding.tvTotalEgresosMonto.text = "-${format.format(totalEgresos)}"
            }
        }
    }

    private fun mostrarDialogoFiltroAvanzado() {
        val opciones = arrayOf(
            "Más recientes primero (Por defecto)",
            "Más antiguos primero",
            "Mayor monto a menor ($$$)",
            "Menor monto a mayor ($)",
            "Orden alfabético (A-Z)"
        )
        val posSeleccionada = when (criterioOrden) {
            "ANTIGUOS" -> 1
            "MAYOR_MONTO" -> 2
            "MENOR_MONTO" -> 3
            "ALFABETICO" -> 4
            else -> 0
        }

        AlertDialog.Builder(mContext)
            .setTitle("Ordenar y Filtrar Movimientos")
            .setSingleChoiceItems(opciones, posSeleccionada) { dialog, which ->
                criterioOrden = when (which) {
                    1 -> "ANTIGUOS"
                    2 -> "MAYOR_MONTO"
                    3 -> "MENOR_MONTO"
                    4 -> "ALFABETICO"
                    else -> "RECIENTES"
                }
                actualizarListaFiltrada()
                Toast.makeText(mContext, "Orden aplicado: ${opciones[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cargarDeudasLocales() {
        lifecycleScope.launch(Dispatchers.IO) {
            val deudasTodas = db.getAllDeudas()
            val gfTodas = db.getAllGastosFijos()
            val deudasPendientes = deudasTodas.filter { (it.totalDeuda - it.totalPagado) > 0.0 }
            withContext(Dispatchers.Main) {
                listaDeudas.clear()
                listaDeudas.addAll(deudasPendientes)
                listaGastosFijos.clear()
                listaGastosFijos.addAll(gfTodas)
            }
        }
    }

    private fun mostrarDialogoMovimiento(movimientoExistente: Movimiento?) {
        val dialogBinding = DialogAgregarMovimientoBinding.inflate(layoutInflater)
        currentDialogBinding = dialogBinding

        val dialog = AlertDialog.Builder(mContext)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cargarDeudasLocales()
        deudaSeleccionada = null
        gastoFijoSeleccionado = null

        var tipoSeleccionado = "GASTO"
        var fechaSeleccionadaTimestamp = System.currentTimeMillis()

        val calActual = Calendar.getInstance()
        val sdfFechaDisplay = SimpleDateFormat("dd MMM, HH:mm 'hrs'", Locale("es", "ES"))
        dialogBinding.tvFechaMovimiento.text = "Hoy, ${String.format("%02d:%02d hrs", calActual.get(Calendar.HOUR_OF_DAY), calActual.get(Calendar.MINUTE))}"

        fun actualizarTabsTipo(tipo: String) {
            tipoSeleccionado = tipo
            val colorGasto = ContextCompat.getColor(mContext, R.color.expense_red)
            val colorIngreso = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val colorTransferencia = ContextCompat.getColor(mContext, R.color.text_primary)
            val colorInactivo = ContextCompat.getColor(mContext, R.color.text_secondary)
            val bgActivo = ContextCompat.getColor(mContext, R.color.surface_card)
            val bgInactivo = ContextCompat.getColor(mContext, android.R.color.transparent)

            dialogBinding.btnTipoGasto.setBackgroundColor(if (tipo == "GASTO") bgActivo else bgInactivo)
            dialogBinding.btnTipoGasto.setTextColor(if (tipo == "GASTO") colorGasto else colorInactivo)
            dialogBinding.btnTipoGasto.elevation = if (tipo == "GASTO") 4f else 0f

            dialogBinding.btnTipoIngreso.setBackgroundColor(if (tipo == "INGRESO") bgActivo else bgInactivo)
            dialogBinding.btnTipoIngreso.setTextColor(if (tipo == "INGRESO") colorIngreso else colorInactivo)
            dialogBinding.btnTipoIngreso.elevation = if (tipo == "INGRESO") 4f else 0f

            dialogBinding.btnTipoTransferencia.setBackgroundColor(if (tipo == "TRANSFERENCIA") bgActivo else bgInactivo)
            dialogBinding.btnTipoTransferencia.setTextColor(if (tipo == "TRANSFERENCIA") colorTransferencia else colorInactivo)
            dialogBinding.btnTipoTransferencia.elevation = if (tipo == "TRANSFERENCIA") 4f else 0f

            dialogBinding.autoTipoMov.setText(tipo, false)
        }

        val nombresDeudas = listaDeudas.map { "${it.concepto} (Resta: $${it.totalDeuda - it.totalPagado})" }
        dialogBinding.autoDeudas.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, nombresDeudas))

        val nombresGastos = listaGastosFijos.map { "${it.concepto} ($${it.monto})" }
        dialogBinding.autoGastosFijos.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, nombresGastos))

        val opcionesQuincena = arrayOf("1ª Quincena", "2ª Quincena")
        dialogBinding.autoQuincena.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, opcionesQuincena))
        
        val diaActual = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val defaultQ = if (diaActual <= 15) "1ª Quincena" else "2ª Quincena"
        dialogBinding.autoQuincena.setText(defaultQ, false)

        dialogBinding.btnTipoGasto.setOnClickListener { actualizarTabsTipo("GASTO") }
        dialogBinding.btnTipoIngreso.setOnClickListener { actualizarTabsTipo("INGRESO") }
        dialogBinding.btnTipoTransferencia.setOnClickListener { actualizarTabsTipo("TRANSFERENCIA") }

        // Category Chips Interactivity
        var categoriaSeleccionadaEnDialogo = movimientoExistente?.categoria ?: "Supermercado"

        fun pintarChipCategoria(categoria: String) {
            categoriaSeleccionadaEnDialogo = categoria
            val bgActivo = ContextCompat.getColor(mContext, R.color.surface_container_low)
            val bgInactivo = ContextCompat.getColor(mContext, R.color.surface_bg)
            val colorActivo = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val colorInactivo = ContextCompat.getColor(mContext, R.color.text_secondary)

            fun setChipStyle(container: View, icon: View?, text: TextView, activo: Boolean) {
                container.backgroundTintList = ColorStateList.valueOf(if (activo) bgActivo else bgInactivo)
                text.setTextColor(if (activo) colorActivo else colorInactivo)
            }

            fun setupChip(container: View, nombreCat: String) {
                val vg = container as? ViewGroup
                if ((vg?.childCount ?: 0) >= 2) {
                    val icon = vg?.getChildAt(0)
                    val tv = vg?.getChildAt(1) as? TextView
                    if (tv != null) {
                        setChipStyle(container, icon, tv, categoria.equals(nombreCat, ignoreCase = true))
                    }
                }
            }

            setupChip(dialogBinding.chipSupermercado, "Supermercado")
            setupChip(dialogBinding.chipServicios, "Servicios")
            setupChip(dialogBinding.chipComida, "Comida")
            setupChip(dialogBinding.chipTransporte, "Transporte")
            setupChip(dialogBinding.chipVivienda, "Vivienda")
            setupChip(dialogBinding.chipOcio, "Ocio")
            setupChip(dialogBinding.chipPeliculas, "Películas")
            setupChip(dialogBinding.chipPrestamo, "Préstamo")
            setupChip(dialogBinding.chipSinCategoria, "Sin categoría")
        }

        dialogBinding.chipSupermercado.setOnClickListener { pintarChipCategoria("Supermercado") }
        dialogBinding.chipServicios.setOnClickListener { pintarChipCategoria("Servicios") }
        dialogBinding.chipComida.setOnClickListener { pintarChipCategoria("Comida") }
        dialogBinding.chipTransporte.setOnClickListener { pintarChipCategoria("Transporte") }
        dialogBinding.chipVivienda.setOnClickListener { pintarChipCategoria("Vivienda") }
        dialogBinding.chipOcio.setOnClickListener { pintarChipCategoria("Ocio") }
        dialogBinding.chipPeliculas.setOnClickListener { pintarChipCategoria("Películas") }
        dialogBinding.chipPrestamo.setOnClickListener { pintarChipCategoria("Préstamo") }
        dialogBinding.chipSinCategoria.setOnClickListener { pintarChipCategoria("Sin categoría") }

        pintarChipCategoria(categoriaSeleccionadaEnDialogo)

        // Selector de Fecha
        dialogBinding.tvFechaMovimiento.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = fechaSeleccionadaTimestamp }
            DatePickerDialog(mContext, { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(mContext, { _, hour, min ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, min)
                    fechaSeleccionadaTimestamp = cal.timeInMillis
                    dialogBinding.tvFechaMovimiento.text = sdfFechaDisplay.format(cal.time)
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Escanear Recibo con IA
        dialogBinding.ivEscanearRecibo.setOnClickListener {
            mostrarOpcionesImagen()
        }

        if (movimientoExistente != null) {
            dialogBinding.tvTituloDialogo.text = "Editar Movimiento"
            dialogBinding.etDescripcion.setText(movimientoExistente.descripcion)
            dialogBinding.etValor.setText(movimientoExistente.valor.toString())
            actualizarTabsTipo(movimientoExistente.tipo_reg.uppercase())
            
            // Precargar timestamp de la fecha del movimiento existente
            if (movimientoExistente.fecha.isNotBlank()) {
                val parsedTs = NotasDatabaseHelper.obtenerTimestampParaOrdenamiento(movimientoExistente)
                if (parsedTs > 0L) {
                    fechaSeleccionadaTimestamp = parsedTs
                    val cal = Calendar.getInstance().apply { timeInMillis = parsedTs }
                    dialogBinding.tvFechaMovimiento.text = sdfFechaDisplay.format(cal.time)
                }
            }

            if (movimientoExistente.tipo_reg == "PAGO DEUDA" || movimientoExistente.tipo_reg == "GASTO FIJO") {
                dialogBinding.cbEsPagoDeuda.visibility = View.GONE 
                dialogBinding.cbEsGastoFijo.visibility = View.GONE
            }
        }

        dialogBinding.cbEsPagoDeuda.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.tilSeleccionDeuda.visibility = if (isChecked) View.VISIBLE else View.GONE
            dialogBinding.tilTipoReg.visibility = if (isChecked || dialogBinding.cbEsGastoFijo.isChecked) View.GONE else View.VISIBLE
            
            if (isChecked) {
                dialogBinding.cbEsGastoFijo.isChecked = false
                gastoFijoSeleccionado = null
                dialogBinding.tilSeleccionGastoFijo.visibility = View.GONE
                dialogBinding.tilSeleccionQuincena.visibility = View.GONE
            }
        }

        dialogBinding.cbEsGastoFijo.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.tilSeleccionGastoFijo.visibility = if (isChecked) View.VISIBLE else View.GONE
            dialogBinding.tilTipoReg.visibility = if (isChecked || dialogBinding.cbEsPagoDeuda.isChecked) View.GONE else View.VISIBLE
            
            if (isChecked) {
                dialogBinding.cbEsPagoDeuda.isChecked = false
                deudaSeleccionada = null
                dialogBinding.tilSeleccionDeuda.visibility = View.GONE
            }
        }

        dialogBinding.autoDeudas.setOnItemClickListener { _, _, pos, _ -> 
            deudaSeleccionada = listaDeudas[pos] 
            dialogBinding.etDescripcion.setText("PAGO DEUDA: ${deudaSeleccionada?.concepto}")
        }

        dialogBinding.autoGastosFijos.setOnItemClickListener { _, _, pos, _ ->
            gastoFijoSeleccionado = listaGastosFijos[pos]
            dialogBinding.tilSeleccionQuincena.visibility = if (gastoFijoSeleccionado?.periodo == "Quincenal") View.VISIBLE else View.GONE
            dialogBinding.etDescripcion.setText("PAGO: ${gastoFijoSeleccionado?.concepto}")
            dialogBinding.etValor.setText(gastoFijoSeleccionado?.monto?.toInt().toString())
        }

        var movEnEdicion: Movimiento? = movimientoExistente

        fun aplicarDatosAlFormulario(datos: DatosTransaccion, comoEdicion: Boolean, movimientoParaEditar: Movimiento?) {
            if (comoEdicion && movimientoParaEditar != null) {
                movEnEdicion = movimientoParaEditar
                dialogBinding.tvTituloDialogo.text = "Actualizar Movimiento (${movimientoParaEditar.id})"
            } else {
                movEnEdicion = null
                dialogBinding.tvTituloDialogo.text = if (movimientoExistente != null) "Editar Movimiento" else "Agregar Movimiento"
            }

            dialogBinding.etDescripcion.setText(datos.descripcion)
            dialogBinding.etValor.setText(datos.valor.toString())
            actualizarTabsTipo(datos.tipo_reg.uppercase())
            pintarChipCategoria(datos.categoria)

            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                val d = sdf.parse(datos.fecha)
                if (d != null) {
                    fechaSeleccionadaTimestamp = d.time
                    dialogBinding.tvFechaMovimiento.text = sdfFechaDisplay.format(d)
                }
            } catch (_: Exception) {}
        }

        onDatosEscaneadosListener = { datos ->
            lifecycleScope.launch(Dispatchers.IO) {
                val duplicado = db.buscarMovimientoDuplicado(datos.valor, datos.tipo_reg, datos.fecha)
                withContext(Dispatchers.Main) {
                    dialogBinding.llLoadingAi.visibility = View.GONE
                    if (duplicado == null) {
                        aplicarDatosAlFormulario(datos, comoEdicion = false, movimientoParaEditar = null)
                        Toast.makeText(mContext, "Comprobante procesado con éxito (ML Kit Offline)", Toast.LENGTH_SHORT).show()
                    } else {
                        mostrarDialogoDuplicado(
                            datos = datos,
                            duplicado = duplicado,
                            onActualizar = {
                                aplicarDatosAlFormulario(datos, comoEdicion = true, movimientoParaEditar = duplicado)
                                Toast.makeText(mContext, "Modo actualización activo para movimiento #${duplicado.id}", Toast.LENGTH_SHORT).show()
                            },
                            onGuardarNuevo = {
                                aplicarDatosAlFormulario(datos, comoEdicion = false, movimientoParaEditar = null)
                                Toast.makeText(mContext, "Listo para guardar como nuevo registro", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        dialogBinding.btnGuardar.setOnClickListener {
            val desc = dialogBinding.etDescripcion.text.toString()
            val valStr = dialogBinding.etValor.text.toString()
            if (desc.isEmpty() || valStr.isEmpty()) {
                Toast.makeText(mContext, "Por favor llene todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val valor = valStr.toIntOrNull() ?: 0
            val fechaMovimiento = Constantes.formatoFecha(fechaSeleccionadaTimestamp)
            val mesAnio = fechaMovimiento.substring(0, 7)

            lifecycleScope.launch(Dispatchers.IO) {
                val edicionActual = movEnEdicion
                if (edicionActual == null) {
                    // CREAR
                    val fc = Constantes.formatoFecha(System.currentTimeMillis())

                    if (dialogBinding.cbEsPagoDeuda.isChecked && deudaSeleccionada != null) {
                        deudaSeleccionada!!.totalPagado += valor
                        db.updateDeuda(deudaSeleccionada!!)
                        db.insertNota(Movimiento(0, desc.uppercase(), valor, "PAGO DEUDA", fechaMovimiento, categoria = categoriaSeleccionadaEnDialogo, fechaCreacion = fc))
                    } else if (dialogBinding.cbEsGastoFijo.isChecked && gastoFijoSeleccionado != null) {
                        val gf = gastoFijoSeleccionado!!
                        var quincena: Int? = null
                        if (gf.periodo == "Quincenal") {
                            val qPos = dialogBinding.autoQuincena.text.toString()
                            if (qPos == "1ª Quincena") {
                                gf.ultimoPagoQ1 = mesAnio
                                quincena = 1
                            } else if (qPos == "2ª Quincena") {
                                gf.ultimoPagoQ2 = mesAnio
                                quincena = 2
                            }
                        } else {
                            gf.ultimoPagoQ1 = mesAnio
                            quincena = 0
                        }
                        db.updateGastoFijo(gf)
                        db.insertNota(Movimiento(0, desc.uppercase(), valor, "GASTO FIJO", fechaMovimiento, gf.id.toInt(), quincena, categoria = categoriaSeleccionadaEnDialogo, fechaCreacion = fc))
                    } else {
                        val tipo = tipoSeleccionado
                        db.insertNota(Movimiento(0, desc.uppercase(), valor, tipo.uppercase(), fechaMovimiento, categoria = categoriaSeleccionadaEnDialogo, fechaCreacion = fc))
                    }
                } else {
                    // EDITAR / ACTUALIZAR
                    val fc = if (edicionActual.fechaCreacion.isNotBlank()) edicionActual.fechaCreacion else Constantes.formatoFecha(System.currentTimeMillis())
                    val nuevoMov = Movimiento(
                        id = edicionActual.id,
                        descripcion = desc.uppercase(),
                        valor = valor,
                        tipo_reg = if (edicionActual.tipo_reg == "PAGO DEUDA") "PAGO DEUDA" else tipoSeleccionado.uppercase(),
                        fecha = fechaMovimiento,
                        gastoFijoId = edicionActual.gastoFijoId,
                        quincenaPagada = edicionActual.quincenaPagada,
                        categoria = categoriaSeleccionadaEnDialogo,
                        fechaCreacion = fc
                    )
                    db.updateMovimiento(nuevoMov)
                }

                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    actualizarListaFiltrada()
                }
            }
        }

        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            currentDialogBinding = null
            onDatosEscaneadosListener = null
        }
        dialog.show()
    }

    private fun mostrarOpcionesImagen() {
        val opciones = arrayOf("Tomar Foto", "Elegir de Galería")
        AlertDialog.Builder(mContext)
            .setTitle("Seleccionar Imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> verificarPermisosYCamara()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun verificarPermisosYCamara() {
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        try {
            val tempFile = File(mContext.cacheDir, "recibo_temp_${System.currentTimeMillis()}.jpg")
            photoUri = FileProvider.getUriForFile(mContext, "${mContext.packageName}.fileprovider", tempFile)
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e("GeminiVision", "Error al abrir la cámara", e)
            Toast.makeText(mContext, "Error al preparar la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoDuplicado(
        datos: DatosTransaccion,
        duplicado: Movimiento,
        onActualizar: () -> Unit,
        onGuardarNuevo: () -> Unit
    ) {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
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

        MaterialAlertDialogBuilder(mContext)
            .setTitle("Movimiento Existente Detectado")
            .setMessage(mensaje)
            .setPositiveButton("Actualizar existente") { _, _ ->
                onActualizar()
            }
            .setNeutralButton("Guardar de nuevo") { _, _ ->
                onGuardarNuevo()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procesarImagenConMLKit(uri: Uri) {
        val binding = currentDialogBinding ?: return
        Log.d("MLKitVision", "Iniciando reconocimiento de comprobante: $uri")

        binding.llLoadingAi.visibility = View.VISIBLE

        try {
            val image = InputImage.fromFilePath(mContext, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val texto = visionText.text
                    Log.d("MLKitVision", "Texto extraído:\n$texto")

                    if (texto.isBlank()) {
                        binding.llLoadingAi.visibility = View.GONE
                        Toast.makeText(mContext, "No se detectó texto en la imagen", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val datos = TransaccionBancariaParser.parsear(texto)
                    if (datos == null || datos.valor <= 0) {
                        binding.llLoadingAi.visibility = View.GONE
                        Toast.makeText(mContext, "No se pudo identificar una transacción bancaria válida en el comprobante", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val listener = onDatosEscaneadosListener
                    if (listener != null) {
                        listener(datos)
                    } else {
                        binding.llLoadingAi.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    binding.llLoadingAi.visibility = View.GONE
                    Log.e("MLKitVision", "Error al procesar con ML Kit", e)
                    Toast.makeText(mContext, "Error al leer comprobante: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            binding.llLoadingAi.visibility = View.GONE
            Log.e("MLKitVision", "Error cargando archivo de imagen", e)
            Toast.makeText(mContext, "Error cargando imagen: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminarMovimiento(movimiento: Movimiento) {
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Movimiento")
            .setMessage("¿Estás seguro de eliminar este movimiento?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.deleteMovimiento(movimiento.id)
                    withContext(Dispatchers.Main) {
                        actualizarListaFiltrada()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
