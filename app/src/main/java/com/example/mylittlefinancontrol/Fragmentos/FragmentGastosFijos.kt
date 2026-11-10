package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.Servicios.GastoFijoWorker
import com.example.mylittlefinancontrol.ui.components.GastoFijoCardItem
import com.example.mylittlefinancontrol.ui.components.MesControlAnualItem
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

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

@Composable
fun GastosFijosStatefulScreen() {
    val context = LocalContext.current
    val db = remember { NotasDatabaseHelper.getInstance(context) }
    val scope = rememberCoroutineScope()

    var listaGastosTodos by remember { mutableStateOf<List<GastoFijo>>(emptyList()) }
    var categoriaFiltroSeleccionada by remember { mutableStateOf("Todos") }
    var filtroEstado by remember { mutableStateOf("TODOS") }
    var criterioOrdenGf by remember { mutableStateOf("DEFAULT") }
    var esOculto by remember { mutableStateOf(false) }

    var showDialogGf by remember { mutableStateOf(false) }
    var gastoEnEdicion by remember { mutableStateOf<GastoFijo?>(null) }
    var gastoControlAnual by remember { mutableStateOf<GastoFijo?>(null) }
    var showResumenMesDialog by remember { mutableStateOf(false) }
    var showFiltroDialog by remember { mutableStateOf(false) }

    fun recargar() {
        scope.launch(Dispatchers.IO) {
            val lista = db.getAllGastosFijos()
            withContext(Dispatchers.Main) {
                listaGastosTodos = lista
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                recargar()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        recargar()
    }

    val filtrados = remember(listaGastosTodos, categoriaFiltroSeleccionada, filtroEstado, criterioOrdenGf) {
        var list = when (categoriaFiltroSeleccionada) {
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

        list = when (filtroEstado) {
            "PENDIENTES" -> list.filter { !estaPagadoTotal(it) }
            "PAGADOS" -> list.filter { estaPagadoTotal(it) }
            "MENSUAL" -> list.filter { it.periodo.equals("Mensual", ignoreCase = true) }
            "QUINCENAL" -> list.filter { it.periodo.equals("Quincenal", ignoreCase = true) }
            else -> list
        }

        when (criterioOrdenGf) {
            "MAYOR_MONTO" -> list.sortedByDescending { it.monto }
            "MENOR_MONTO" -> list.sortedBy { it.monto }
            "ALFABETICO" -> list.sortedBy { it.concepto }
            else -> list
        }
    }

    GastosFijosScreen(
        listaTodos = listaGastosTodos,
        listaFiltrada = filtrados,
        categoriaSeleccionada = categoriaFiltroSeleccionada,
        esOculto = esOculto,
        onTogglePrivacidad = { esOculto = !esOculto },
        onSelectCategoria = { categoriaFiltroSeleccionada = it },
        onOpenCalendarResumen = { showResumenMesDialog = true },
        onOpenFilter = { showFiltroDialog = true },
        onAgregarGasto = {
            gastoEnEdicion = null
            showDialogGf = true
        },
        onEditarGasto = { gasto ->
            gastoEnEdicion = gasto
            showDialogGf = true
        },
        onEliminarGasto = { gasto ->
            db.deleteGastoFijo(gasto.id)
            recargar()
        },
        onControlAnualGasto = { gasto -> gastoControlAnual = gasto }
    )

    if (showResumenMesDialog) {
        val cal = Calendar.getInstance()
        val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val nombresMeses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        val nombreMes = nombresMeses[cal.get(Calendar.MONTH)]
        val pagados = listaGastosTodos.count { g ->
            if (g.periodo == "Quincenal") g.ultimoPagoQ1 == mesAnioActual && g.ultimoPagoQ2 == mesAnioActual else g.ultimoPagoQ1 == mesAnioActual
        }
        val pendientes = listaGastosTodos.size - pagados
        val totalMonto = listaGastosTodos.sumOf { it.monto }
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        AlertDialog(
            onDismissRequest = { showResumenMesDialog = false },
            title = { Text("Compromisos de $nombreMes ${cal.get(Calendar.YEAR)}", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "• Total compromisos: ${listaGastosTodos.size}\n" +
                        "• Pagados este mes: $pagados\n" +
                        "• Pendientes: $pendientes\n" +
                        "• Presupuesto fijo total: ${format.format(totalMonto)}"
                )
            },
            confirmButton = {
                TextButton(onClick = { showResumenMesDialog = false }) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showFiltroDialog) {
        val opciones = listOf(
            "Todos los compromisos (Por defecto)",
            "Solo pendientes de pago",
            "Solo pagados este mes",
            "Solo frecuencia mensual",
            "Solo frecuencia quincenal",
            "Ordenar: Mayor a menor monto ($$$)",
            "Ordenar: Menor a mayor monto ($)",
            "Ordenar: Alfabéticamente (A-Z)"
        )
        AlertDialog(
            onDismissRequest = { showFiltroDialog = false },
            title = { Text("Filtrar y Ordenar Gastos Fijos", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    opciones.forEachIndexed { which, label ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
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
                                    showFiltroDialog = false
                                    Toast.makeText(context, "Filtro aplicado: $label", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFiltroDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDialogGf) {
        DialogAgregarEditarGastoFijo(
            db = db,
            gastoExistente = gastoEnEdicion,
            onDismiss = { showDialogGf = false },
            onSave = { gasto, esEdicion, mantenerAbierto ->
                if (!esEdicion) {
                    db.insertGastoFijo(gasto)
                } else {
                    db.updateGastoFijo(gasto)
                }
                programarVerificacionGastoFijoWorker(context)
                recargar()
                if (!mantenerAbierto) {
                    showDialogGf = false
                }
            }
        )
    }

    gastoControlAnual?.let { gasto ->
        DialogControlAnualGasto(
            db = db,
            gasto = gasto,
            onDismiss = {
                gastoControlAnual = null
                recargar()
            }
        )
    }
}

@Composable
fun GastosFijosScreen(
    listaTodos: List<GastoFijo>,
    listaFiltrada: List<GastoFijo>,
    categoriaSeleccionada: String,
    esOculto: Boolean,
    onTogglePrivacidad: () -> Unit,
    onSelectCategoria: (String) -> Unit,
    onOpenCalendarResumen: () -> Unit,
    onOpenFilter: () -> Unit,
    onAgregarGasto: () -> Unit,
    onEditarGasto: (GastoFijo) -> Unit,
    onEliminarGasto: (GastoFijo) -> Unit,
    onControlAnualGasto: (GastoFijo) -> Unit
) {
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val total = listaTodos.sumOf { it.monto }
    val cal = Calendar.getInstance()
    val hoyDia = cal.get(Calendar.DAY_OF_MONTH)
    val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

    var totalPagado = 0.0
    for (g in listaTodos) {
        if (g.periodo.contains("Quincenal", true)) {
            if (g.ultimoPagoQ1 == mesAnioActual) totalPagado += (g.monto / 2)
            if (g.ultimoPagoQ2 == mesAnioActual) totalPagado += (g.monto / 2)
        } else {
            if (g.ultimoPagoQ1 == mesAnioActual) totalPagado += g.monto
        }
    }
    val totalPendiente = (total - totalPagado).coerceAtLeast(0.0)
    val progreso = if (total > 0) ((totalPagado / total) * 100).toInt().coerceIn(0, 100) else 0

    val proximosDias = listaTodos.mapNotNull { g ->
        val d = Regex("\\d+").find(g.diaPago)?.value?.toIntOrNull()
        d?.let { Pair(it, g) }
    }.sortedBy { it.first }
    val proximo = proximosDias.firstOrNull { it.first >= hoyDia } ?: proximosDias.firstOrNull()
    val proximoVencimientoTexto = if (proximo != null) {
        val diasRestantes = if (proximo.first >= hoyDia) proximo.first - hoyDia else (30 - hoyDia) + proximo.first
        val diasTxt = when (diasRestantes) {
            0 -> "Hoy"
            1 -> "Mañana"
            else -> "En $diasRestantes días"
        }
        "Día ${proximo.first} · $diasTxt"
    } else {
        "Sin vencimientos pendientes"
    }

    val countServicios = listaTodos.count { it.categoria.contains("serv", true) || it.categoria.contains("luz", true) || it.categoria.contains("agua", true) }
    val countVivienda = listaTodos.count { it.categoria.contains("viv", true) || it.categoria.contains("arriend", true) || it.categoria.contains("casa", true) }
    val countTelecom = listaTodos.count { it.categoria.contains("tele", true) || it.categoria.contains("inter", true) || it.categoria.contains("fibra", true) || it.categoria.contains("wifi", true) }
    val countStreaming = listaTodos.count { it.categoria.contains("stream", true) || it.categoria.contains("suscrip", true) }
    val countEducacion = listaTodos.count { it.categoria.contains("educ", true) || it.categoria.contains("univ", true) }
    val countOtros = listaTodos.count {
        !it.categoria.contains("serv", true) &&
            !it.categoria.contains("viv", true) &&
            !it.categoria.contains("tele", true) &&
            !it.categoria.contains("stream", true) &&
            !it.categoria.contains("educ", true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gastos Fijos",
                            color = colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${listaTodos.size} compromisos recurrentes",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onTogglePrivacidad,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceCard)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (esOculto) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                                ),
                                contentDescription = "Privacidad",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenCalendarResumen,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceCard)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_month),
                                contentDescription = "Calendario",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenFilter,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceCard)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tune),
                                contentDescription = "Filtrar",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Hero Card Presupuesto Fijo
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRESUPUESTO FIJO MENSUAL",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.ghostBg
                            ) {
                                Text(
                                    text = proximoVencimientoTexto,
                                    color = colors.primaryEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (esOculto) "$ ••••••••" else format.format(total),
                            color = colors.textPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progreso / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = colors.primaryEmerald,
                            trackColor = colors.surfaceContainerHigh
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (esOculto) "Pagado: $ ••••" else "Pagado: ${format.format(totalPagado)}",
                                color = colors.primaryEmerald,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$progreso% completado",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (esOculto) "Pendiente: $ ••••" else "Pendiente: ${format.format(totalPendiente)}",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            // Chips de Categorías
            item {
                val chips = listOf(
                    "Todos" to "Todos (${listaTodos.size})",
                    "Servicios" to "Servicios ($countServicios)",
                    "Vivienda" to "Vivienda ($countVivienda)",
                    "Telecom" to "Telecom ($countTelecom)",
                    "Streaming" to "Streaming ($countStreaming)",
                    "Educación" to "Educación ($countEducacion)",
                    "Otros" to "Otros ($countOtros)"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { (key, label) ->
                        val selected = categoriaSeleccionada == key
                        Surface(
                            modifier = Modifier.clickable { onSelectCategoria(key) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) colors.primaryEmerald else colors.surfaceCard,
                            border = BorderStroke(1.dp, if (selected) colors.primaryEmerald else colors.borderSubtle)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            items(listaFiltrada, key = { it.id.ifEmpty { it.concepto } }) { gasto ->
                GastoFijoCardItem(
                    gasto = gasto,
                    onEditarClick = { onEditarGasto(gasto) },
                    onEliminarClick = { onEliminarGasto(gasto) },
                    onControlAnualClick = { onControlAnualGasto(gasto) }
                )
            }
        }

        FloatingActionButton(
            onClick = onAgregarGasto,
            containerColor = colors.primaryEmerald,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Agregar Gasto Fijo"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogAgregarEditarGastoFijo(
    db: NotasDatabaseHelper,
    gastoExistente: GastoFijo?,
    onDismiss: () -> Unit,
    onSave: (GastoFijo, esEdicion: Boolean, mantenerAbierto: Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = DabaTheme.colors

    val listaCategoriasGf = remember { db.obtenerCategoriasPorModulo("GASTO_FIJO") }
    var categoriaSel by remember {
        mutableStateOf(gastoExistente?.categoria ?: listaCategoriasGf.firstOrNull() ?: "Otros")
    }
    var expandedCategoria by remember { mutableStateOf(false) }
    var nuevaCategoriaInput by remember { mutableStateOf("") }

    var concepto by remember { mutableStateOf(gastoExistente?.concepto ?: "") }
    var montoStr by remember {
        mutableStateOf(
            if (gastoExistente != null) {
                if (gastoExistente.monto % 1.0 == 0.0) gastoExistente.monto.toLong().toString() else gastoExistente.monto.toString()
            } else ""
        )
    }

    val periodos = remember { listOf("Mensual", "Quincenal (Q1/Q2)", "Bimestral", "Anual") }
    var periodoSel by remember {
        mutableStateOf(
            if (gastoExistente != null) {
                if (gastoExistente.periodo.contains("Quincenal", true)) "Quincenal (Q1/Q2)" else gastoExistente.periodo
            } else periodos[0]
        )
    }
    var expandedPeriodo by remember { mutableStateOf(false) }

    val diasPago = remember {
        listOf(
            "Día 01",
            "Día 05 de cada mes",
            "Día 10 de cada mes",
            "Día 15 de cada mes",
            "Día 20 de cada mes",
            "Día 30 (Fin de mes)"
        )
    }
    var diaPagoSel by remember {
        mutableStateOf(
            if (gastoExistente != null) {
                diasPago.find { it.contains(gastoExistente.diaPago) } ?: "Día ${gastoExistente.diaPago} de cada mes"
            } else diasPago[1]
        )
    }
    var expandedDiaPago by remember { mutableStateOf(false) }
    var recordatorio by remember { mutableStateOf(gastoExistente?.recordatorio ?: true) }
    var showConfirmAnother by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (gastoExistente != null) "Editar Gasto Fijo" else "Nuevo Gasto Fijo",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Cerrar",
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto (Ej. Internet, Arriendo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    label = { Text("Monto ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedCategoria,
                    onExpandedChange = { expandedCategoria = !expandedCategoria }
                ) {
                    OutlinedTextField(
                        value = categoriaSel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoria,
                        onDismissRequest = { expandedCategoria = false }
                    ) {
                        listaCategoriasGf.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    categoriaSel = cat
                                    expandedCategoria = false
                                }
                            )
                        }
                    }
                }

                if (categoriaSel == "Otra...") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nuevaCategoriaInput,
                        onValueChange = { nuevaCategoriaInput = it },
                        label = { Text("Nueva categoría") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedPeriodo,
                    onExpandedChange = { expandedPeriodo = !expandedPeriodo }
                ) {
                    OutlinedTextField(
                        value = periodoSel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frecuencia") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPeriodo,
                        onDismissRequest = { expandedPeriodo = false }
                    ) {
                        periodos.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    periodoSel = p
                                    expandedPeriodo = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDiaPago,
                    onExpandedChange = { expandedDiaPago = !expandedDiaPago }
                ) {
                    OutlinedTextField(
                        value = diaPagoSel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Día de pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDiaPago) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDiaPago,
                        onDismissRequest = { expandedDiaPago = false }
                    ) {
                        diasPago.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d) },
                                onClick = {
                                    diaPagoSel = d
                                    expandedDiaPago = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recordatorio de vencimiento",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = recordatorio,
                        onCheckedChange = { recordatorio = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.primaryEmerald)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = colors.textPrimary)
                    }
                    Button(
                        onClick = {
                            val monto = Constantes.limpiarYParsearMonto(montoStr)
                            var categoriaFinal = categoriaSel
                            if (categoriaSel == "Otra...") {
                                if (nuevaCategoriaInput.isBlank()) {
                                    Toast.makeText(context, "Escribe el nombre de la nueva categoría", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                db.insertarCategoria(nuevaCategoriaInput.trim(), "GASTO_FIJO")
                                categoriaFinal = nuevaCategoriaInput.trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            }
                            val diaNum = Regex("\\d+").find(diaPagoSel)?.value ?: "05"
                            val periodo = if (periodoSel.contains("Quincenal", true)) "Quincenal" else periodoSel

                            if (concepto.isNotBlank() && monto > 0) {
                                if (gastoExistente == null) {
                                    onSave(
                                        GastoFijo("", concepto.trim(), categoriaFinal, monto, diaNum, periodo, recordatorio = recordatorio),
                                        false,
                                        true
                                    )
                                    showConfirmAnother = true
                                } else {
                                    gastoExistente.concepto = concepto.trim()
                                    gastoExistente.monto = monto
                                    gastoExistente.diaPago = diaNum
                                    gastoExistente.categoria = categoriaFinal
                                    gastoExistente.periodo = periodo
                                    gastoExistente.recordatorio = recordatorio
                                    onSave(gastoExistente, true, false)
                                }
                            } else {
                                Toast.makeText(context, "Por favor complete el concepto y un monto válido", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
                    ) {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showConfirmAnother) {
        AlertDialog(
            onDismissRequest = {
                showConfirmAnother = false
                onDismiss()
            },
            title = { Text("¡Guardado con éxito!", fontWeight = FontWeight.Bold) },
            text = { Text("¿Quieres agregar otro gasto fijo?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        concepto = ""
                        montoStr = ""
                        showConfirmAnother = false
                    }
                ) {
                    Text("Sí, agregar otro", color = colors.primaryEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmAnother = false
                        onDismiss()
                    }
                ) {
                    Text("No, terminar")
                }
            }
        )
    }
}

@Composable
private fun DialogControlAnualGasto(
    db: NotasDatabaseHelper,
    gasto: GastoFijo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val calActual = Calendar.getInstance()
    val anioActual = calActual.get(Calendar.YEAR)
    val mesActual = calActual.get(Calendar.MONTH)
    val esQuincenal = gasto.periodo.contains("Quincenal", true)
    val montoMensualTotal = if (esQuincenal) gasto.monto * 2 else gasto.monto

    var refreshKey by remember { mutableIntStateOf(0) }
    var mesParaPagar by remember { mutableStateOf<Pair<Int, Int>?>(null) } // numeroMes to cuotasPagadas
    var mesParaDesmarcar by remember { mutableStateOf<Int?>(null) }

    val nombresMeses = remember {
        arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    }

    data class MesEstadoUI(
        val numeroMes: Int,
        val nombreMes: String,
        val cuotasPagadas: Int,
        val totalCuotas: Int,
        val fechaPagoTexto: String
    )

    val listaMeses = remember(gasto, refreshKey) {
        val todosMovimientos = db.getAllNotas()
        val gfIdInt = gasto.id.toIntOrNull() ?: -1
        (1..12).map { m ->
            val mesClave = String.format(Locale.US, "%04d-%02d", anioActual, m)
            val movsEncontrados = todosMovimientos.filter { mov ->
                val idCoincide = (mov.gastoFijoId == gfIdInt && gfIdInt != -1) ||
                    (mov.descripcion.contains(gasto.concepto, ignoreCase = true) && mov.tipo_reg.contains("GASTO", true))
                val fechaEfectiva = if (mov.fecha.isNotBlank()) mov.fecha else mov.fechaCreacion
                idCoincide && fechaEfectiva.startsWith(mesClave)
            }

            var q1Pagada = movsEncontrados.any { it.quincenaPagada == 1 } || db.esPagoManualControlAnual(gasto.id, mesClave, 1)
            var q2Pagada = movsEncontrados.any { it.quincenaPagada == 2 } || db.esPagoManualControlAnual(gasto.id, mesClave, 2)

            if (!esQuincenal) {
                if (movsEncontrados.isNotEmpty() || db.esPagoManualControlAnual(gasto.id, mesClave, 0) || db.esPagoManualControlAnual(gasto.id, mesClave, 1)) {
                    q1Pagada = true
                } else if (gasto.ultimoPagoQ1.startsWith(mesClave)) {
                    q1Pagada = true
                }
            } else {
                if (!q1Pagada && gasto.ultimoPagoQ1.startsWith(mesClave)) q1Pagada = true
                if (!q2Pagada && gasto.ultimoPagoQ2.startsWith(mesClave)) q2Pagada = true
            }

            val totalCuotasMes = if (esQuincenal) 2 else 1
            var cuotasPagadasMes = 0
            if (esQuincenal) {
                if (q1Pagada) cuotasPagadasMes++
                if (q2Pagada) cuotasPagadasMes++
            } else {
                if (q1Pagada) cuotasPagadasMes = 1
            }

            val detalle = when {
                cuotasPagadasMes == totalCuotasMes -> {
                    val mov = movsEncontrados.firstOrNull()
                    if (mov != null && mov.fecha.isNotBlank()) {
                        "Pagado el ${mov.fecha.split(" ")[0]} (${format.format(montoMensualTotal)})"
                    } else {
                        "Pagado en $mesClave (${format.format(montoMensualTotal)})"
                    }
                }
                cuotasPagadasMes > 0 -> "🟡 Parcial ($cuotasPagadasMes/$totalCuotasMes pagada) · Resta ${format.format(gasto.monto)}"
                else -> if (m <= (mesActual + 1)) "⏱️ Pendiente (${format.format(montoMensualTotal)})" else "Próximo pago (${format.format(montoMensualTotal)})"
            }

            MesEstadoUI(m, "${nombresMeses[m - 1]} $anioActual", cuotasPagadasMes, totalCuotasMes, detalle)
        }
    }

    val mesesTranscurridos = mesActual + 1
    val mesesCompletos = listaMeses.take(mesesTranscurridos).count { it.cuotasPagadas == it.totalCuotas }
    val mesesPendientes = mesesTranscurridos - mesesCompletos

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceCard,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📅 Control Anual $anioActual",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (esQuincenal) {
                                "${gasto.concepto} · ${format.format(montoMensualTotal)} / mes (${format.format(gasto.monto)} x 2 quincenas)"
                            } else {
                                "${gasto.concepto} · ${format.format(montoMensualTotal)} / mes"
                            },
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Cerrar",
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (mesesPendientes <= 0) {
                            "🟢 ¡Vas 100% al día con tus pagos de $anioActual!"
                        } else {
                            "🔴 Tienes $mesesPendientes mes(es) pendiente(s) de pago este año"
                        },
                        color = if (mesesPendientes <= 0) colors.primaryEmerald else colors.expenseRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listaMeses, key = { it.numeroMes }) { item ->
                        val esPagadoMes = item.cuotasPagadas == item.totalCuotas
                        val badgeTexto = when {
                            esPagadoMes -> "✓ PAGADO (${item.cuotasPagadas}/${item.totalCuotas})"
                            item.cuotasPagadas > 0 -> "🟡 PARCIAL (${item.cuotasPagadas}/${item.totalCuotas})"
                            else -> "⏱️ PENDIENTE"
                        }
                        MesControlAnualItem(
                            nombreMes = item.nombreMes,
                            detallePago = item.fechaPagoTexto,
                            badgeEstadoTexto = badgeTexto,
                            esPagado = esPagadoMes,
                            mostrarBotonPagar = !esPagadoMes,
                            onPagarClick = { mesParaPagar = item.numeroMes to item.cuotasPagadas },
                            modifier = Modifier.clickable(enabled = esPagadoMes) {
                                mesParaDesmarcar = item.numeroMes
                            }
                        )
                    }
                }
            }
        }
    }

    mesParaPagar?.let { (numMes, cuotasPagadas) ->
        val opcionesPago = listOf(
            "Solo marcar como Pagado (Checklist histórico)",
            "Registrar movimiento y descontar dinero"
        )
        AlertDialog(
            onDismissRequest = { mesParaPagar = null },
            title = { Text("Marcar pago de ${nombresMeses[numMes - 1]}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    opcionesPago.forEachIndexed { which, label ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val mesFormatted = String.format(Locale.US, "%02d", numMes)
                                    val mesClave = "$anioActual-$mesFormatted"
                                    val diaNum = gasto.diaPago.trim().filter { it.isDigit() }.toIntOrNull() ?: 15
                                    val fechaPago = "$anioActual-$mesFormatted-${String.format(Locale.US, "%02d", diaNum)} 12:00:00"
                                    val montoAAnadir = if (esQuincenal) gasto.monto else montoMensualTotal
                                    val quincenaAAnadir = if (esQuincenal) (if (cuotasPagadas == 0) 1 else 2) else 0

                                    db.marcarPagoManualControlAnual(gasto.id, mesClave, quincenaAAnadir, true)
                                    if (quincenaAAnadir == 1 || !esQuincenal) gasto.ultimoPagoQ1 = mesClave
                                    if (quincenaAAnadir == 2 || !esQuincenal) gasto.ultimoPagoQ2 = mesClave
                                    db.updateGastoFijo(gasto)

                                    if (which == 1) {
                                        val gfIdInt = gasto.id.toIntOrNull() ?: -1
                                        val nuevoMov = Movimiento(
                                            id = 0,
                                            descripcion = "PAGO: ${gasto.concepto}".uppercase(),
                                            valor = montoAAnadir.toInt(),
                                            tipo_reg = "GASTO FIJO",
                                            fecha = fechaPago,
                                            gastoFijoId = gfIdInt,
                                            quincenaPagada = quincenaAAnadir,
                                            categoria = gasto.categoria,
                                            fechaCreacion = Constantes.formatoFecha(System.currentTimeMillis())
                                        )
                                        db.insertNota(nuevoMov)
                                        Toast.makeText(context, "Pago registrado con movimiento en dinero", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Mes de ${nombresMeses[numMes - 1]} marcado como pagado (Checklist)", Toast.LENGTH_SHORT).show()
                                    }
                                    mesParaPagar = null
                                    refreshKey++
                                }
                                .padding(vertical = 12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mesParaPagar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    mesParaDesmarcar?.let { numMes ->
        AlertDialog(
            onDismissRequest = { mesParaDesmarcar = null },
            title = { Text("Desmarcar pago de ${nombresMeses[numMes - 1]}", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas desmarcar este mes y volver a colocarlo como PENDIENTE?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mesClave = String.format(Locale.US, "%04d-%02d", anioActual, numMes)
                        db.marcarPagoManualControlAnual(gasto.id, mesClave, 0, false)
                        db.marcarPagoManualControlAnual(gasto.id, mesClave, 1, false)
                        db.marcarPagoManualControlAnual(gasto.id, mesClave, 2, false)
                        if (gasto.ultimoPagoQ1.startsWith(mesClave)) gasto.ultimoPagoQ1 = ""
                        if (gasto.ultimoPagoQ2.startsWith(mesClave)) gasto.ultimoPagoQ2 = ""
                        db.updateGastoFijo(gasto)
                        Toast.makeText(context, "Mes de ${nombresMeses[numMes - 1]} vuelto a colocar en PENDIENTE", Toast.LENGTH_SHORT).show()
                        mesParaDesmarcar = null
                        refreshKey++
                    }
                ) {
                    Text("Desmarcar", color = colors.expenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mesParaDesmarcar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GastosFijosScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        val sample = listOf(
            GastoFijo("1", "Internet Fibra Óptica", "Telecom", 125000.0, "15", "Mensual", recordatorio = true)
        )
        GastosFijosScreen(
            listaTodos = sample,
            listaFiltrada = sample,
            categoriaSeleccionada = "Todos",
            esOculto = false,
            onTogglePrivacidad = {},
            onSelectCategoria = {},
            onOpenCalendarResumen = {},
            onOpenFilter = {},
            onAgregarGasto = {},
            onEditarGasto = {},
            onEliminarGasto = {},
            onControlAnualGasto = {}
        )
    }
}
