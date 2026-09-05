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
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

    // IA y Cámara
    private var currentDialogBinding: DialogAgregarMovimientoBinding? = null
    private var photoUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { procesarImagenConIA(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { procesarImagenConIA(it) }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

        setupFiltros()
        setupRecyclerView()
        cargarDeudasLocales()

        binding.FABAgregarNota.setOnClickListener {
            mostrarDialogoMovimiento(null)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDeudasLocales()
        actualizarListaFiltrada()
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

            actualizarListaFiltrada()
        }

        binding.chipFiltroTodos.setOnClickListener { actualizarChipsUI("TODOS") }
        binding.chipFiltroGastosFijos.setOnClickListener { actualizarChipsUI("GASTOS FIJOS") }
        binding.chipFiltroSupermercado.setOnClickListener { actualizarChipsUI("SUPERMERCADO") }
        binding.chipFiltroIngresos.setOnClickListener { actualizarChipsUI("INGRESOS") }
        binding.chipFiltroDeudas.setOnClickListener { actualizarChipsUI("DEUDAS") }
    }

    private fun setupRecyclerView() {
        notasAdaptador = NotasAdaptador(
            mutableListOf(),
            mContext,
            onEditarClick = { mov -> mostrarDialogoMovimiento(mov) },
            onEliminarClick = { mov -> eliminarMovimiento(mov) }
        )
        binding.notasRv.layoutManager = LinearLayoutManager(mContext)
        binding.notasRv.adapter = notasAdaptador
        actualizarListaFiltrada()
    }

    private fun actualizarListaFiltrada() {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val todos = db.getAllNotas()
        val filtrados = todos.filter { mov ->
            try {
                val parsed = extraerAnioYMes(mov.fecha)
                val coincideFecha = when {
                    mesSeleccionado == -1 && anioSeleccionado == -1 -> true
                    mesSeleccionado == -1 && anioSeleccionado != -1 -> (parsed == null || parsed.first == anioSeleccionado)
                    mesSeleccionado != -1 && anioSeleccionado == -1 -> (parsed != null && parsed.second == mesSeleccionado)
                    else -> (parsed != null && parsed.first == anioSeleccionado && parsed.second == mesSeleccionado)
                }

                val coincideBusqueda = searchQuery.isEmpty() || mov.descripcion.contains(searchQuery, ignoreCase = true)

                val coincideCategoria = when (categoriaFiltro) {
                    "GASTOS FIJOS" -> mov.tipo_reg.contains("GASTO FIJO", ignoreCase = true) || mov.descripcion.contains("PAGO:", ignoreCase = true)
                    "SUPERMERCADO" -> mov.descripcion.contains("SUPERMERCADO", ignoreCase = true) || mov.descripcion.contains("DESPENSA", ignoreCase = true) || mov.descripcion.contains("LÍDER", ignoreCase = true) || mov.descripcion.contains("ALIMENT", ignoreCase = true) || mov.descripcion.contains("COMIDA", ignoreCase = true)
                    "INGRESOS" -> mov.tipo_reg.contains("INGRESO", ignoreCase = true) || mov.descripcion.contains("HONORARIOS", ignoreCase = true) || mov.descripcion.contains("SUELDO", ignoreCase = true)
                    "DEUDAS" -> mov.tipo_reg.contains("PAGO DEUDA", ignoreCase = true) || mov.tipo_reg.contains("DEUDA", ignoreCase = true)
                    else -> true
                }

                coincideFecha && coincideBusqueda && coincideCategoria
            } catch (e: Exception) { true }
        }

        notasAdaptador.refrescarLista(filtrados)

        val totalIngresos = filtrados.filter { it.tipo_reg.contains("INGRESO", ignoreCase = true) }.sumOf { it.valor.toDouble() }
        val totalEgresos = filtrados.filter { !it.tipo_reg.contains("INGRESO", ignoreCase = true) }.sumOf { it.valor.toDouble() }
        val balancePeriodo = totalIngresos - totalEgresos

        saldoRealTexto = format.format(if (balancePeriodo >= 0) balancePeriodo else -balancePeriodo)
        if (!esSaldoOculto) {
            val totalPeriodo = if (totalEgresos > 0) totalEgresos else (if (totalIngresos > 0) totalIngresos else 0.0)
            binding.tvTotalMovMonto.text = format.format(totalPeriodo)
        }

        binding.tvConteoRegistros.text = "${filtrados.size} Registros"
        binding.tvTotalIngresosMonto.text = "+${format.format(totalIngresos)}"
        binding.tvTotalEgresosMonto.text = "-${format.format(totalEgresos)}"
    }

    private fun cargarDeudasLocales() {
        listaDeudas.clear()
        listaDeudas.addAll(db.getAllDeudas())
        listaGastosFijos.clear()
        listaGastosFijos.addAll(db.getAllGastosFijos())
    }

    private fun mostrarDialogoMovimiento(movimientoExistente: Movimiento?) {
        val dialogBinding = DialogAgregarMovimientoBinding.inflate(layoutInflater)
        currentDialogBinding = dialogBinding

        val bottomSheetDialog = BottomSheetDialog(mContext, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(dialogBinding.root)

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
        fun pintarChipCategoria(categoria: String) {
            val bgActivo = ContextCompat.getColor(mContext, R.color.surface_container_low)
            val bgInactivo = ContextCompat.getColor(mContext, R.color.surface_bg)
            val colorActivo = ContextCompat.getColor(mContext, R.color.primary_emerald)
            val colorInactivo = ContextCompat.getColor(mContext, R.color.text_secondary)

            fun setChipStyle(container: View, icon: View?, text: TextView, activo: Boolean) {
                container.backgroundTintList = ColorStateList.valueOf(if (activo) bgActivo else bgInactivo)
                text.setTextColor(if (activo) colorActivo else colorInactivo)
            }

            val tvSuper = dialogBinding.chipSupermercado.getChildAt(1) as TextView
            val tvServ = dialogBinding.chipServicios.getChildAt(1) as TextView
            val tvComida = dialogBinding.chipComida.getChildAt(1) as TextView
            val tvTransp = dialogBinding.chipTransporte.getChildAt(1) as TextView

            setChipStyle(dialogBinding.chipSupermercado, dialogBinding.chipSupermercado.getChildAt(0), tvSuper, categoria == "Supermercado")
            setChipStyle(dialogBinding.chipServicios, dialogBinding.chipServicios.getChildAt(0), tvServ, categoria == "Servicios")
            setChipStyle(dialogBinding.chipComida, dialogBinding.chipComida.getChildAt(0), tvComida, categoria == "Comida")
            setChipStyle(dialogBinding.chipTransporte, dialogBinding.chipTransporte.getChildAt(0), tvTransp, categoria == "Transporte")
        }

        dialogBinding.chipSupermercado.setOnClickListener {
            pintarChipCategoria("Supermercado")
            if (dialogBinding.etDescripcion.text.isNullOrEmpty() || dialogBinding.etDescripcion.text.toString().contains("Servicios|Comida|Transporte".toRegex())) {
                dialogBinding.etDescripcion.setText("Supermercado")
            }
        }
        dialogBinding.chipServicios.setOnClickListener {
            pintarChipCategoria("Servicios")
            if (dialogBinding.etDescripcion.text.isNullOrEmpty() || dialogBinding.etDescripcion.text.toString().contains("Supermercado|Comida|Transporte".toRegex())) {
                dialogBinding.etDescripcion.setText("Servicios")
            }
        }
        dialogBinding.chipComida.setOnClickListener {
            pintarChipCategoria("Comida")
            if (dialogBinding.etDescripcion.text.isNullOrEmpty() || dialogBinding.etDescripcion.text.toString().contains("Supermercado|Servicios|Transporte".toRegex())) {
                dialogBinding.etDescripcion.setText("Comida")
            }
        }
        dialogBinding.chipTransporte.setOnClickListener {
            pintarChipCategoria("Transporte")
            if (dialogBinding.etDescripcion.text.isNullOrEmpty() || dialogBinding.etDescripcion.text.toString().contains("Supermercado|Servicios|Comida".toRegex())) {
                dialogBinding.etDescripcion.setText("Transporte")
            }
        }

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

        dialogBinding.btnGuardar.setOnClickListener {
            val desc = dialogBinding.etDescripcion.text.toString()
            val valStr = dialogBinding.etValor.text.toString()
            if (desc.isEmpty() || valStr.isEmpty()) {
                Toast.makeText(mContext, "Por favor llene todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val valor = valStr.toIntOrNull() ?: 0
            
            if (movimientoExistente == null) {
                // CREAR
                val fecha = Constantes.formatoFecha(fechaSeleccionadaTimestamp) // yyyy-MM-dd HH:mm:ss
                val mesAnio = fecha.substring(0, 7) // yyyy-MM

                if (dialogBinding.cbEsPagoDeuda.isChecked && deudaSeleccionada != null) {
                    deudaSeleccionada!!.totalPagado += valor
                    db.updateDeuda(deudaSeleccionada!!)
                    db.insertNota(Movimiento(0, desc.uppercase(), valor, "PAGO DEUDA", fecha))
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
                    db.insertNota(Movimiento(0, desc.uppercase(), valor, "GASTO FIJO", fecha, gf.id.toInt(), quincena))
                } else {
                    val tipo = tipoSeleccionado
                    db.insertNota(Movimiento(0, desc.uppercase(), valor, tipo.uppercase(), fecha))
                }
            } else {
                // EDITAR
                val nuevoMov = Movimiento(
                    movimientoExistente.id,
                    desc.uppercase(),
                    valor,
                    if (movimientoExistente.tipo_reg == "PAGO DEUDA") "PAGO DEUDA" else tipoSeleccionado.uppercase(),
                    movimientoExistente.fecha
                )
                db.updateMovimiento(nuevoMov)
            }
            
            bottomSheetDialog.dismiss()
            actualizarListaFiltrada()
        }

        dialogBinding.btnCancelar.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.show()
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

    private fun procesarImagenConIA(uri: Uri) {
        val binding = currentDialogBinding ?: return
        
        Log.d("GeminiVision", "Iniciando procesamiento de imagen: $uri")
        
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            Log.e("GeminiVision", "Error: API Key de Gemini está vacía")
            Toast.makeText(mContext, "Error: API Key de Gemini no configurada", Toast.LENGTH_LONG).show()
            return
        }

        binding.llLoadingAi.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                Log.d("GeminiVision", "Cargando bitmap desde URI...")
                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream = mContext.contentResolver.openInputStream(uri)
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    if (originalBitmap == null) {
                        Log.e("GeminiVision", "Bitmap original es nulo")
                        return@withContext null
                    }

                    Log.d("GeminiVision", "Bitmap original cargado: ${originalBitmap.width}x${originalBitmap.height}")

                    // Escalar para optimizar memoria
                    if (originalBitmap.width > 1024 || originalBitmap.height > 1024) {
                        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                        val width = if (ratio > 1) 1024 else (1024 * ratio).toInt()
                        val height = if (ratio > 1) (1024 / ratio).toInt() else 1024
                        Bitmap.createScaledBitmap(originalBitmap, width, height, true)
                    } else {
                        originalBitmap
                    }
                }

                if (bitmap == null) {
                    Log.e("GeminiVision", "Error: El bitmap final es nulo")
                    Toast.makeText(mContext, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val prompt = """
                    Analiza este recibo. Extrae:
                    1. 'concepto': Descripción corta de la compra.
                    2. 'valor_total': El monto total como número puro.
                    Responde SOLO en JSON: {"concepto": "...", "valor_total": "..."}
                """.trimIndent()

                val safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
                )

                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    safetySettings = safetySettings
                )

                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(content {
                        image(bitmap)
                        text(prompt)
                    })
                }

                val rawText = response.text
                if (rawText.isNullOrBlank()) {
                    Toast.makeText(mContext, "La IA no pudo procesar esta imagen", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val cleanJson = rawText.substringAfter("{").substringBeforeLast("}").let { "{ $it }" }
                val jsonResponse = JSONObject(cleanJson)
                
                binding.etDescripcion.setText(jsonResponse.optString("concepto"))
                binding.etValor.setText(jsonResponse.optString("valor_total"))
                
            } catch (e: Exception) {
                Log.e("GeminiVision", "Error en IA: ${e.message}", e)
                Toast.makeText(mContext, "Error IA: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.llLoadingAi.visibility = View.GONE
            }
        }
    }

    private fun eliminarMovimiento(movimiento: Movimiento) {
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Movimiento")
            .setMessage("¿Estás seguro de eliminar este movimiento?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteMovimiento(movimiento.id)
                actualizarListaFiltrada()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
