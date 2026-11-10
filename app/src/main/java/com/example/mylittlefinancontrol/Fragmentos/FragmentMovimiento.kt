package com.example.mylittlefinancontrol.Fragmentos

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.Modelos.Ahorro
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.MovimientoListItem
import com.example.mylittlefinancontrol.NotasAdaptador
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.Servicios.DatosTransaccion
import com.example.mylittlefinancontrol.Servicios.TransaccionBancariaParser
import com.example.mylittlefinancontrol.ui.components.MovimientoHeaderItem
import com.example.mylittlefinancontrol.ui.components.MovimientoItem
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
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
                    if (p0 > 1000) return Pair(p0, p1 - 1)
                    else if (p2 > 1000) return Pair(p2, p1 - 1)
                }
            }
        }
    }
    return null
}

private fun evaluarCoincideCategoria(
    mov: Movimiento,
    categoria: String,
    tipoUpper: String = mov.tipo_reg.uppercase(),
    descUpper: String = mov.descripcion.uppercase(),
    catUpper: String = mov.categoria.uppercase()
): Boolean {
    if (categoria == "TODOS") return true

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

@Composable
fun MovimientoStatefulScreen(onNavigateAhorros: () -> Unit) {
    val context = LocalContext.current
    val db = remember { NotasDatabaseHelper.getInstance(context) }
    val scope = rememberCoroutineScope()

    var todosMovimientos by remember { mutableStateOf<List<Movimiento>>(emptyList()) }
    var listaDeudas by remember { mutableStateOf<List<Deuda>>(emptyList()) }
    var listaGastosFijos by remember { mutableStateOf<List<GastoFijo>>(emptyList()) }
    var listaAhorros by remember { mutableStateOf<List<Ahorro>>(emptyList()) }

    var mesSeleccionado by remember { mutableIntStateOf(-1) }
    var anioSeleccionado by remember { mutableIntStateOf(-1) }
    var categoriaFiltro by remember { mutableStateOf("TODOS") }
    var searchQuery by remember { mutableStateOf("") }
    var criterioOrden by remember { mutableStateOf("RECIENTES") }
    var esSaldoOculto by remember { mutableStateOf(false) }

    val pasoPaginacion = 10
    var cantidadAMostrar by remember { mutableIntStateOf(pasoPaginacion) }

    var showDialogMovimiento by remember { mutableStateOf(false) }
    var movEnEdicion by remember { mutableStateOf<Movimiento?>(null) }
    var movParaEliminar by remember { mutableStateOf<Movimiento?>(null) }
    var showSelectorMes by remember { mutableStateOf(false) }
    var showSelectorAnio by remember { mutableStateOf(false) }
    var showFiltroAvanzado by remember { mutableStateOf(false) }

    fun recargarTodo() {
        scope.launch(Dispatchers.IO) {
            val notas = db.getAllNotas()
            val deudas = db.getAllDeudas().filter { (it.totalDeuda - it.totalPagado) > 0.0 }
            val gf = db.getAllGastosFijos()
            val ah = db.getAllAhorros()
            withContext(Dispatchers.Main) {
                todosMovimientos = notas
                listaDeudas = deudas
                listaGastosFijos = gf
                listaAhorros = ah
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                recargarTodo()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        recargarTodo()
    }

    val baseFechaBusqueda = remember(todosMovimientos, mesSeleccionado, anioSeleccionado, searchQuery) {
        todosMovimientos.filter { mov ->
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
            } catch (_: Exception) {
                true
            }
        }
    }

    val conteosCategorias = remember(baseFechaBusqueda) {
        val categoriasList = arrayOf("GASTOS FIJOS", "SUPERMERCADO", "INGRESOS", "DEUDAS", "SERVICIOS", "COMIDA", "TRANSPORTE", "SALUD", "STREAMING", "EDUCACION", "OTROS")
        val map = mutableMapOf<String, Int>()
        for (mov in baseFechaBusqueda) {
            val tUpper = mov.tipo_reg.uppercase()
            val dUpper = mov.descripcion.uppercase()
            val cUpper = mov.categoria.uppercase()
            for (cat in categoriasList) {
                if (evaluarCoincideCategoria(mov, cat, tUpper, dUpper, cUpper)) {
                    map[cat] = (map[cat] ?: 0) + 1
                }
            }
        }
        map
    }

    val listaTotalFiltrada = remember(baseFechaBusqueda, categoriaFiltro, criterioOrden) {
        val filtrados = baseFechaBusqueda.filter { evaluarCoincideCategoria(it, categoriaFiltro) }
        when (criterioOrden) {
            "ANTIGUOS" -> filtrados.reversed()
            "MAYOR_MONTO" -> filtrados.sortedByDescending { it.valor }
            "MENOR_MONTO" -> filtrados.sortedBy { it.valor }
            "ALFABETICO" -> filtrados.sortedBy { it.descripcion }
            else -> filtrados
        }
    }

    val sublista = remember(listaTotalFiltrada, cantidadAMostrar) {
        listaTotalFiltrada.take(cantidadAMostrar)
    }

    val displayItems = remember(sublista) {
        NotasAdaptador.procesarGrupos(sublista)
    }

    val mesesDisplay = remember {
        arrayOf("Todos los meses", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    }
    val mesTexto = if (mesSeleccionado == -1) "Todos los meses" else mesesDisplay[mesSeleccionado + 1]
    val anioTexto = if (anioSeleccionado == -1) "Todos" else anioSeleccionado.toString()

    MovimientoScreen(
        baseCount = baseFechaBusqueda.size,
        conteosMap = conteosCategorias,
        listaTotalFiltrada = listaTotalFiltrada,
        sublistaCount = sublista.size,
        displayItems = displayItems,
        mesTexto = mesTexto,
        anioTexto = anioTexto,
        categoriaFiltro = categoriaFiltro,
        searchQuery = searchQuery,
        esSaldoOculto = esSaldoOculto,
        pasoPaginacion = pasoPaginacion,
        onOpenSelectorMes = { showSelectorMes = true },
        onOpenSelectorAnio = { showSelectorAnio = true },
        onExportarReporte = {
            val mTxt = if (mesSeleccionado == -1) "Histórico completo" else mesesDisplay[mesSeleccionado + 1]
            val aTxt = if (anioSeleccionado == -1) "todos los años" else anioSeleccionado.toString()
            Toast.makeText(context, "Reporte de $mTxt $aTxt exportado", Toast.LENGTH_SHORT).show()
        },
        onTogglePrivacidad = { esSaldoOculto = !esSaldoOculto },
        onSearchChange = {
            searchQuery = it
            cantidadAMostrar = pasoPaginacion
        },
        onOpenFiltroAvanzado = { showFiltroAvanzado = true },
        onSelectCategoria = {
            categoriaFiltro = it
            cantidadAMostrar = pasoPaginacion
        },
        onCargarMas = {
            if (cantidadAMostrar < listaTotalFiltrada.size) {
                cantidadAMostrar += pasoPaginacion
            }
        },
        onAgregarMovimiento = {
            movEnEdicion = null
            showDialogMovimiento = true
        },
        onEditarMovimiento = { mov ->
            movEnEdicion = mov
            showDialogMovimiento = true
        },
        onEliminarMovimiento = { mov -> movParaEliminar = mov }
    )

    if (showSelectorMes) {
        AlertDialog(
            onDismissRequest = { showSelectorMes = false },
            title = { Text("Seleccionar Periodo", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    mesesDisplay.forEachIndexed { which, label ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mesSeleccionado = if (which == 0) -1 else which - 1
                                    cantidadAMostrar = pasoPaginacion
                                    showSelectorMes = false
                                }
                                .padding(vertical = 10.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectorMes = false }) { Text("Cancelar") }
            }
        )
    }

    if (showSelectorAnio) {
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        val aniosDisplay = listOf("Todos los años", (anioActual - 2).toString(), (anioActual - 1).toString(), anioActual.toString(), (anioActual + 1).toString(), (anioActual + 2).toString())
        AlertDialog(
            onDismissRequest = { showSelectorAnio = false },
            title = { Text("Seleccionar Año", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    aniosDisplay.forEachIndexed { which, label ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    anioSeleccionado = if (which == 0) -1 else label.toInt()
                                    cantidadAMostrar = pasoPaginacion
                                    showSelectorAnio = false
                                }
                                .padding(vertical = 10.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectorAnio = false }) { Text("Cancelar") }
            }
        )
    }

    if (showFiltroAvanzado) {
        val opciones = listOf(
            "RECIENTES" to "Más recientes primero (Por defecto)",
            "ANTIGUOS" to "Más antiguos primero",
            "MAYOR_MONTO" to "Mayor monto a menor ($$$)",
            "MENOR_MONTO" to "Menor monto a mayor ($)",
            "ALFABETICO" to "Orden alfabético (A-Z)"
        )
        AlertDialog(
            onDismissRequest = { showFiltroAvanzado = false },
            title = { Text("Ordenar y Filtrar Movimientos", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    opciones.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    criterioOrden = key
                                    cantidadAMostrar = pasoPaginacion
                                    showFiltroAvanzado = false
                                    Toast.makeText(context, "Orden aplicado: $label", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = criterioOrden == key, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFiltroAvanzado = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDialogMovimiento) {
        DialogAgregarEditarMovimiento(
            db = db,
            movimientoExistente = movEnEdicion,
            listaDeudas = listaDeudas,
            listaGastosFijos = listaGastosFijos,
            listaAhorros = listaAhorros,
            onNavigateAhorros = {
                showDialogMovimiento = false
                onNavigateAhorros()
            },
            onDismiss = { showDialogMovimiento = false },
            onSaved = { mantenerAbierto ->
                recargarTodo()
                if (!mantenerAbierto) {
                    showDialogMovimiento = false
                }
            }
        )
    }

    movParaEliminar?.let { mov ->
        AlertDialog(
            onDismissRequest = { movParaEliminar = null },
            title = { Text("Eliminar Movimiento", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de eliminar este movimiento?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = mov
                        movParaEliminar = null
                        scope.launch(Dispatchers.IO) {
                            db.eliminarMovimientoTransaccional(target)
                            withContext(Dispatchers.Main) {
                                recargarTodo()
                            }
                        }
                    }
                ) {
                    Text("Eliminar", color = DabaTheme.colors.expenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { movParaEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun MovimientoScreen(
    baseCount: Int,
    conteosMap: Map<String, Int>,
    listaTotalFiltrada: List<Movimiento>,
    sublistaCount: Int,
    displayItems: List<MovimientoListItem>,
    mesTexto: String,
    anioTexto: String,
    categoriaFiltro: String,
    searchQuery: String,
    esSaldoOculto: Boolean,
    pasoPaginacion: Int,
    onOpenSelectorMes: () -> Unit,
    onOpenSelectorAnio: () -> Unit,
    onExportarReporte: () -> Unit,
    onTogglePrivacidad: () -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenFiltroAvanzado: () -> Unit,
    onSelectCategoria: (String) -> Unit,
    onCargarMas: () -> Unit,
    onAgregarMovimiento: () -> Unit,
    onEditarMovimiento: (Movimiento) -> Unit,
    onEliminarMovimiento: (Movimiento) -> Unit
) {
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val totalIngresos = listaTotalFiltrada.filter { it.tipo_reg.contains("INGRESO", ignoreCase = true) }.sumOf { it.valor.toDouble() }
    val totalEgresos = listaTotalFiltrada.filter { !it.tipo_reg.contains("INGRESO", ignoreCase = true) }.sumOf { it.valor.toDouble() }
    val montoAMostrar = when (categoriaFiltro) {
        "INGRESOS" -> totalIngresos
        "TODOS" -> if (totalEgresos > 0) totalEgresos else totalIngresos
        else -> totalEgresos
    }

    val tituloBalanceMov = when (categoriaFiltro) {
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

    val cantTotal = listaTotalFiltrada.size
    val conteoRegistrosTexto = when {
        cantTotal == 0 -> "0 Registros"
        sublistaCount < cantTotal -> "$sublistaCount de $cantTotal Registros"
        cantTotal == 1 -> "1 Registro"
        else -> "$cantTotal Registros"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp)
        ) {
            // 1. Hero Card Resumen Periodo
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = onOpenSelectorMes,
                                    shape = RoundedCornerShape(50),
                                    color = colors.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_calendar_month),
                                            contentDescription = null,
                                            tint = colors.primaryEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = mesTexto,
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onOpenSelectorAnio,
                                    shape = RoundedCornerShape(50),
                                    color = colors.surfaceContainerLow
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = anioTexto,
                                            color = colors.textSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = onExportarReporte,
                                shape = CircleShape,
                                color = colors.surfaceContainerLow,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_share),
                                        contentDescription = "Exportar",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tituloBalanceMov,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(colors.primaryEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = conteoRegistrosTexto,
                                        color = colors.primaryEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (esSaldoOculto) "••••••••" else format.format(montoAMostrar),
                                color = colors.textPrimary,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onTogglePrivacidad,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (esSaldoOculto) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                                    ),
                                    contentDescription = "Privacidad",
                                    tint = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                color = colors.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(colors.primaryEmerald50),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_arrow_downward),
                                            contentDescription = null,
                                            tint = colors.primaryEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Ingresos", color = colors.textSecondary, fontSize = 10.sp)
                                        Text(
                                            text = "+${format.format(totalIngresos)}",
                                            color = colors.primaryEmerald,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                color = colors.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(colors.expenseRedBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_arrow_upward),
                                            contentDescription = null,
                                            tint = colors.expenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Egresos", color = colors.textSecondary, fontSize = 10.sp)
                                        Text(
                                            text = "-${format.format(totalEgresos)}",
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Buscador y Chips de Categorías
            item {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(50),
                        color = colors.surfaceCard,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Buscar por concepto o comercio...",
                                        color = colors.textMuted,
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchChange,
                                    singleLine = true,
                                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                                    cursorBrush = SolidColor(colors.primaryEmerald),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(
                                onClick = onOpenFiltroAvanzado,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tune),
                                    contentDescription = "Ordenar",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    fun conteo(cat: String) = conteosMap[cat] ?: 0
                    val chips = listOf(
                        "TODOS" to "Todos ($baseCount)",
                        "GASTOS FIJOS" to "Gastos Fijos (${conteo("GASTOS FIJOS")})",
                        "SUPERMERCADO" to "Supermercado (${conteo("SUPERMERCADO")})",
                        "INGRESOS" to "Ingresos (${conteo("INGRESOS")})",
                        "DEUDAS" to "Deudas (${conteo("DEUDAS")})",
                        "SERVICIOS" to "Servicios (${conteo("SERVICIOS")})",
                        "COMIDA" to "Comida (${conteo("COMIDA")})",
                        "TRANSPORTE" to "Transporte (${conteo("TRANSPORTE")})",
                        "SALUD" to "Salud (${conteo("SALUD")})",
                        "STREAMING" to "Streaming (${conteo("STREAMING")})",
                        "EDUCACION" to "Educación (${conteo("EDUCACION")})",
                        "OTROS" to "Otros (${conteo("OTROS")})"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chips.forEach { (key, label) ->
                            val active = categoriaFiltro == key
                            Surface(
                                modifier = Modifier.clickable { onSelectCategoria(key) },
                                shape = RoundedCornerShape(50),
                                color = if (active) colors.primaryEmerald else colors.surfaceCard,
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) Color.White else colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Lista de Movimientos con Headers por Día
            items(
                items = displayItems,
                key = { item ->
                    when (item) {
                        is MovimientoListItem.Header -> "header_${item.fechaTitulo}"
                        is MovimientoListItem.Item -> "mov_${item.movimiento.id}_${item.movimiento.fecha}"
                    }
                }
            ) { item ->
                when (item) {
                    is MovimientoListItem.Header -> {
                        MovimientoHeaderItem(
                            fechaTitulo = item.fechaTitulo,
                            conteoTexto = item.conteoTexto
                        )
                    }
                    is MovimientoListItem.Item -> {
                        MovimientoItem(
                            movimiento = item.movimiento,
                            onEditarClick = { onEditarMovimiento(item.movimiento) },
                            onEliminarClick = { onEliminarMovimiento(item.movimiento) }
                        )
                    }
                }
            }

            // 4. Botón Cargar Más o Indicador de Final de Lista
            item {
                if (sublistaCount < listaTotalFiltrada.size) {
                    val restantes = listaTotalFiltrada.size - sublistaCount
                    val proximoPaso = if (restantes > pasoPaginacion) pasoPaginacion else restantes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = onCargarMas,
                            shape = RoundedCornerShape(50),
                            color = colors.surfaceCard,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_autorenew),
                                    contentDescription = null,
                                    tint = colors.primaryEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cargar $proximoPaso anteriores (quedan $restantes)",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (listaTotalFiltrada.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_done_all),
                                contentDescription = null,
                                tint = colors.primaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Has revisado todos los movimientos del periodo",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Todos los registros cargados exitosamente",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAgregarMovimiento,
            containerColor = colors.primaryEmerald,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Agregar movimiento"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogAgregarEditarMovimiento(
    db: NotasDatabaseHelper,
    movimientoExistente: Movimiento?,
    listaDeudas: List<Deuda>,
    listaGastosFijos: List<GastoFijo>,
    listaAhorros: List<Ahorro>,
    onNavigateAhorros: () -> Unit,
    onDismiss: () -> Unit,
    onSaved: (mantenerAbierto: Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = DabaTheme.colors
    val scope = rememberCoroutineScope()
    val sdfFechaDisplay = remember { SimpleDateFormat("dd MMM, HH:mm 'hrs'", Locale("es", "ES")) }

    var movEnEdicion by remember { mutableStateOf(movimientoExistente) }
    var tipoSeleccionado by remember { mutableStateOf(movimientoExistente?.tipo_reg?.uppercase() ?: "GASTO") }
    var descripcion by remember { mutableStateOf(movimientoExistente?.descripcion ?: "") }
    var valorStr by remember { mutableStateOf(movimientoExistente?.valor?.toString() ?: "") }

    val initialTs = remember {
        if (movimientoExistente != null && movimientoExistente.fecha.isNotBlank()) {
            val parsed = NotasDatabaseHelper.obtenerTimestampParaOrdenamiento(movimientoExistente)
            if (parsed > 0L) parsed else System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }
    }
    var fechaTimestamp by remember { mutableLongStateOf(initialTs) }
    var fechaTexto by remember {
        mutableStateOf(
            if (movimientoExistente != null) {
                sdfFechaDisplay.format(Date(initialTs))
            } else {
                val cal = Calendar.getInstance()
                "Hoy, ${String.format(Locale.US, "%02d:%02d hrs", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}"
            }
        )
    }

    val listaCategorias = remember { db.obtenerCategoriasPorModulo("MOVIMIENTO") }
    var categoriaSel by remember {
        mutableStateOf(movimientoExistente?.categoria ?: listaCategorias.firstOrNull() ?: "Varios")
    }
    var expandedCategoria by remember { mutableStateOf(false) }
    var nuevaCategoriaInput by remember { mutableStateOf("") }

    val metodosPagoList = remember { db.obtenerMetodosPago() }
    val nombresMetodos = remember { metodosPagoList.map { it.nombreFormateado() } }
    val metodoPorDefecto = remember { metodosPagoList.find { it.esPredeterminado } ?: metodosPagoList.firstOrNull() }
    var metodoPagoSel by remember {
        mutableStateOf(movimientoExistente?.metodoPago ?: metodoPorDefecto?.nombreFormateado() ?: "Efectivo")
    }
    var expandedMetodoPago by remember { mutableStateOf(false) }

    var esPagoDeuda by remember { mutableStateOf(false) }
    var deudaSeleccionada by remember { mutableStateOf<Deuda?>(null) }
    var expandedDeuda by remember { mutableStateOf(false) }

    var esGastoFijo by remember { mutableStateOf(false) }
    var gastoFijoSeleccionado by remember { mutableStateOf<GastoFijo?>(null) }
    var expandedGf by remember { mutableStateOf(false) }

    val diaActual = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    var quincenaSel by remember { mutableStateOf(if (diaActual <= 15) "1ª Quincena" else "2ª Quincena") }
    var expandedQuincena by remember { mutableStateOf(false) }

    var ahorroSeleccionado by remember { mutableStateOf<Ahorro?>(null) }
    var expandedAhorro by remember { mutableStateOf(false) }

    var isProcessingOcr by remember { mutableStateOf(false) }
    var showSelectorImagen by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var duplicadoDetectado by remember { mutableStateOf<Pair<Movimiento, () -> Unit>?>(null) }
    var showConfirmAnother by remember { mutableStateOf(false) }

    fun aplicarDatosEscaneados(datos: DatosTransaccion, comoEdicion: Boolean, movParaEditar: Movimiento?) {
        movEnEdicion = if (comoEdicion) movParaEditar else null
        descripcion = datos.descripcion
        valorStr = datos.valor.toString()
        tipoSeleccionado = datos.tipo_reg.uppercase()
        categoriaSel = datos.categoria
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val d = sdf.parse(datos.fecha)
            if (d != null) {
                fechaTimestamp = d.time
                fechaTexto = sdfFechaDisplay.format(d)
            }
        } catch (_: Exception) {}
    }

    fun procesarImagenUri(uri: Uri) {
        isProcessingOcr = true
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val texto = visionText.text
                    if (texto.isBlank()) {
                        isProcessingOcr = false
                        Toast.makeText(context, "No se detectó texto en la imagen", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val datos = TransaccionBancariaParser.parsear(texto)
                    if (datos == null || datos.valor <= 0) {
                        isProcessingOcr = false
                        Toast.makeText(context, "No se pudo identificar una transacción bancaria válida", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }
                    scope.launch(Dispatchers.IO) {
                        val dup = db.buscarMovimientoDuplicado(datos.valor, datos.fecha)
                        withContext(Dispatchers.Main) {
                            isProcessingOcr = false
                            if (dup == null) {
                                aplicarDatosEscaneados(datos, false, null)
                                Toast.makeText(context, "Comprobante procesado con éxito (ML Kit Offline)", Toast.LENGTH_SHORT).show()
                            } else {
                                aplicarDatosEscaneados(datos, true, dup)
                                Toast.makeText(context, "Movimiento existente detectado (#${dup.id})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    isProcessingOcr = false
                    Toast.makeText(context, "Error al leer comprobante: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            isProcessingOcr = false
            Toast.makeText(context, "Error cargando imagen: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { procesarImagenUri(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { procesarImagenUri(it) }
        }
    }

    fun abrirCamara() {
        try {
            val tempFile = File(context.cacheDir, "recibo_temp_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            photoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e("MLKitVision", "Error al abrir la cámara", e)
            Toast.makeText(context, "Error al preparar la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) abrirCamara() else Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

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
                // Header + Scan Receipt button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (movEnEdicion != null) "Editar Movimiento" else "Agregar Movimiento",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSelectorImagen = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_receipt_long),
                                contentDescription = "Escanear recibo",
                                tint = colors.primaryEmerald
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
                }

                if (isProcessingOcr) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.primaryEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analizando comprobante con IA...", color = colors.primaryEmerald, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs GASTO / INGRESO / TRANSFERENCIA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("GASTO", "INGRESO", "TRANSFERENCIA").forEach { t ->
                        val active = tipoSeleccionado == t
                        val textColor = when {
                            !active -> colors.textSecondary
                            t == "GASTO" -> colors.expenseRed
                            t == "INGRESO" -> colors.primaryEmerald
                            else -> colors.textPrimary
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tipoSeleccionado = t },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) colors.surfaceCard else Color.Transparent,
                            shadowElevation = if (active) 2.dp else 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t,
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = valorStr,
                    onValueChange = { valorStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Monto ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción / Concepto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Categoría
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
                        listaCategorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    categoriaSel = cat
                                    if (cat.equals("Ahorro", ignoreCase = true)) {
                                        esPagoDeuda = false
                                        esGastoFijo = false
                                    } else {
                                        ahorroSeleccionado = null
                                    }
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

                if (categoriaSel.equals("Ahorro", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedAhorro,
                        onExpandedChange = { expandedAhorro = !expandedAhorro }
                    ) {
                        OutlinedTextField(
                            value = ahorroSeleccionado?.let { "${it.titulo} (${it.ubicacion})" } ?: "Seleccionar meta de ahorro",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Meta de ahorro") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAhorro) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAhorro,
                            onDismissRequest = { expandedAhorro = false }
                        ) {
                            listaAhorros.forEach { ah ->
                                DropdownMenuItem(
                                    text = { Text("${ah.titulo} (${ah.ubicacion})") },
                                    onClick = {
                                        ahorroSeleccionado = ah
                                        descripcion = "ABONO: ${ah.titulo}"
                                        expandedAhorro = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Crear nueva caja de ahorro...", color = colors.primaryEmerald, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    expandedAhorro = false
                                    onNavigateAhorros()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Método de Pago
                ExposedDropdownMenuBox(
                    expanded = expandedMetodoPago,
                    onExpandedChange = { expandedMetodoPago = !expandedMetodoPago }
                ) {
                    OutlinedTextField(
                        value = metodoPagoSel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método de Pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMetodoPago) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMetodoPago,
                        onDismissRequest = { expandedMetodoPago = false }
                    ) {
                        nombresMetodos.forEach { mp ->
                            DropdownMenuItem(
                                text = { Text(mp) },
                                onClick = {
                                    metodoPagoSel = mp
                                    expandedMetodoPago = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha y Hora
                OutlinedTextField(
                    value = fechaTexto,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Fecha y Hora") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = fechaTimestamp }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    cal.set(Calendar.YEAR, y)
                                    cal.set(Calendar.MONTH, m)
                                    cal.set(Calendar.DAY_OF_MONTH, d)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, min ->
                                            cal.set(Calendar.HOUR_OF_DAY, hour)
                                            cal.set(Calendar.MINUTE, min)
                                            fechaTimestamp = cal.timeInMillis
                                            fechaTexto = sdfFechaDisplay.format(cal.time)
                                            quincenaSel = if (d <= 15) "1ª Quincena" else "2ª Quincena"
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = colors.textPrimary,
                        disabledBorderColor = colors.borderSubtle,
                        disabledLabelColor = colors.textSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (movimientoExistente == null || (movimientoExistente.tipo_reg != "PAGO DEUDA" && movimientoExistente.tipo_reg != "GASTO FIJO")) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = esPagoDeuda,
                            onCheckedChange = {
                                esPagoDeuda = it
                                if (it) {
                                    esGastoFijo = false
                                    gastoFijoSeleccionado = null
                                } else {
                                    deudaSeleccionada = null
                                }
                            }
                        )
                        Text("Asociar a pago de deuda", color = colors.textPrimary, fontSize = 12.sp)
                    }

                    if (esPagoDeuda && listaDeudas.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedDeuda,
                            onExpandedChange = { expandedDeuda = !expandedDeuda }
                        ) {
                            OutlinedTextField(
                                value = deudaSeleccionada?.concepto ?: "Seleccionar deuda",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Deuda pendiente") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDeuda) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDeuda,
                                onDismissRequest = { expandedDeuda = false }
                            ) {
                                listaDeudas.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("${d.concepto} (Resta: $${(d.totalDeuda - d.totalPagado).toLong()})") },
                                        onClick = {
                                            deudaSeleccionada = d
                                            descripcion = "PAGO DEUDA: ${d.concepto}"
                                            expandedDeuda = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = esGastoFijo,
                            onCheckedChange = {
                                esGastoFijo = it
                                if (it) {
                                    esPagoDeuda = false
                                    deudaSeleccionada = null
                                } else {
                                    gastoFijoSeleccionado = null
                                }
                            }
                        )
                        Text("Asociar a gasto fijo", color = colors.textPrimary, fontSize = 12.sp)
                    }

                    if (esGastoFijo && listaGastosFijos.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedGf,
                            onExpandedChange = { expandedGf = !expandedGf }
                        ) {
                            OutlinedTextField(
                                value = gastoFijoSeleccionado?.concepto ?: "Seleccionar gasto fijo",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gasto fijo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGf) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedGf,
                                onDismissRequest = { expandedGf = false }
                            ) {
                                listaGastosFijos.forEach { gf ->
                                    DropdownMenuItem(
                                        text = { Text("${gf.concepto} ($${gf.monto.toLong()})") },
                                        onClick = {
                                            gastoFijoSeleccionado = gf
                                            descripcion = "PAGO: ${gf.concepto}"
                                            valorStr = gf.monto.toInt().toString()
                                            expandedGf = false
                                        }
                                    )
                                }
                            }
                        }

                        if (gastoFijoSeleccionado?.periodo == "Quincenal") {
                            Spacer(modifier = Modifier.height(6.dp))
                            ExposedDropdownMenuBox(
                                expanded = expandedQuincena,
                                onExpandedChange = { expandedQuincena = !expandedQuincena }
                            ) {
                                OutlinedTextField(
                                    value = quincenaSel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Quincena") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQuincena) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedQuincena,
                                    onDismissRequest = { expandedQuincena = false }
                                ) {
                                    listOf("1ª Quincena", "2ª Quincena").forEach { q ->
                                        DropdownMenuItem(
                                            text = { Text(q) },
                                            onClick = {
                                                quincenaSel = q
                                                expandedQuincena = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                            if (descripcion.isBlank() || valorStr.isBlank()) {
                                Toast.makeText(context, "Por favor llene todos los campos", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            var categoriaFinal = categoriaSel
                            if (categoriaSel == "Otra...") {
                                if (nuevaCategoriaInput.isBlank()) {
                                    Toast.makeText(context, "Escribe el nombre de la nueva categoría", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                db.insertarCategoria(nuevaCategoriaInput.trim(), "MOVIMIENTO")
                                categoriaFinal = nuevaCategoriaInput.trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            }
                            val valor = valorStr.toIntOrNull() ?: 0
                            val fechaMovimiento = Constantes.formatoFecha(fechaTimestamp)
                            val mesAnio = fechaMovimiento.substring(0, 7)

                            fun ejecutarGuardado(movAEditar: Movimiento?) {
                                scope.launch(Dispatchers.IO) {
                                    if (movAEditar == null) {
                                        val fc = Constantes.formatoFecha(System.currentTimeMillis())
                                        if (esPagoDeuda && deudaSeleccionada != null) {
                                            val movDeuda = Movimiento(0, descripcion.uppercase(), valor, "PAGO DEUDA", fechaMovimiento, categoria = categoriaFinal, fechaCreacion = fc, metodoPago = metodoPagoSel)
                                            db.insertarAbonoDeudaTransaccional(movDeuda, deudaSeleccionada!!.id)
                                        } else if (ahorroSeleccionado != null) {
                                            val movAhorro = Movimiento(0, descripcion.uppercase(), valor, "AHORRO", fechaMovimiento, categoria = categoriaFinal, fechaCreacion = fc, metodoPago = metodoPagoSel)
                                            db.insertarAbonoAhorroTransaccional(movAhorro, ahorroSeleccionado!!.id.toIntOrNull() ?: -1)
                                        } else if (esGastoFijo && gastoFijoSeleccionado != null) {
                                            val gf = gastoFijoSeleccionado!!
                                            var quincena: Int? = null
                                            if (gf.periodo == "Quincenal") {
                                                if (quincenaSel == "1ª Quincena") {
                                                    gf.ultimoPagoQ1 = mesAnio
                                                    quincena = 1
                                                } else {
                                                    gf.ultimoPagoQ2 = mesAnio
                                                    quincena = 2
                                                }
                                            } else {
                                                gf.ultimoPagoQ1 = mesAnio
                                                quincena = 0
                                            }
                                            db.updateGastoFijo(gf)
                                            db.insertNota(Movimiento(0, descripcion.uppercase(), valor, "GASTO FIJO", fechaMovimiento, gf.id.toInt(), quincena, categoria = categoriaFinal, fechaCreacion = fc, metodoPago = metodoPagoSel))
                                        } else {
                                            db.insertNota(Movimiento(0, descripcion.uppercase(), valor, tipoSeleccionado.uppercase(), fechaMovimiento, categoria = categoriaFinal, fechaCreacion = fc, metodoPago = metodoPagoSel))
                                        }
                                    } else {
                                        val fc = movAEditar.fechaCreacion.ifBlank { Constantes.formatoFecha(System.currentTimeMillis()) }
                                        val nuevoMov = Movimiento(
                                            id = movAEditar.id,
                                            descripcion = descripcion.uppercase(),
                                            valor = valor,
                                            tipo_reg = if (movAEditar.tipo_reg == "PAGO DEUDA") "PAGO DEUDA" else tipoSeleccionado.uppercase(),
                                            fecha = fechaMovimiento,
                                            gastoFijoId = movAEditar.gastoFijoId,
                                            quincenaPagada = movAEditar.quincenaPagada,
                                            categoria = categoriaFinal,
                                            fechaCreacion = fc,
                                            metodoPago = metodoPagoSel
                                        )
                                        db.updateMovimiento(nuevoMov)
                                    }

                                    withContext(Dispatchers.Main) {
                                        if (movAEditar == null) {
                                            onSaved(true)
                                            showConfirmAnother = true
                                        } else {
                                            onSaved(false)
                                        }
                                    }
                                }
                            }

                            val edicionActual = movEnEdicion
                            if (edicionActual == null) {
                                scope.launch(Dispatchers.IO) {
                                    val dup = db.buscarMovimientoDuplicado(valor, fechaMovimiento)
                                    withContext(Dispatchers.Main) {
                                        if (dup != null) {
                                            duplicadoDetectado = dup to { ejecutarGuardado(null) }
                                        } else {
                                            ejecutarGuardado(null)
                                        }
                                    }
                                }
                            } else {
                                ejecutarGuardado(edicionActual)
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

    if (showSelectorImagen) {
        AlertDialog(
            onDismissRequest = { showSelectorImagen = false },
            title = { Text("Seleccionar Imagen", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Tomar Foto",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSelectorImagen = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    abrirCamara()
                                } else {
                                    cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = "Elegir de Galería",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSelectorImagen = false
                                galleryLauncher.launch("image/*")
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectorImagen = false }) { Text("Cancelar") }
            }
        )
    }

    duplicadoDetectado?.let { (dup, onGuardarNuevo) ->
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        AlertDialog(
            onDismissRequest = { duplicadoDetectado = null },
            title = { Text("Movimiento Existente Detectado", fontWeight = FontWeight.Bold) },
            text = {
                Text("Ya se registró un movimiento por ${format.format(dup.valor)} (${dup.descripcion}). ¿Deseas guardarlo de nuevo?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        duplicadoDetectado = null
                        onGuardarNuevo()
                    }
                ) {
                    Text("Guardar de nuevo", color = colors.primaryEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicadoDetectado = null }) { Text("Cancelar") }
            }
        )
    }

    if (showConfirmAnother) {
        AlertDialog(
            onDismissRequest = {
                showConfirmAnother = false
                onDismiss()
            },
            title = { Text("¡Guardado con éxito!", fontWeight = FontWeight.Bold) },
            text = { Text("¿Quieres agregar otro registro?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        descripcion = ""
                        valorStr = ""
                        esPagoDeuda = false
                        esGastoFijo = false
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

@Preview(showBackground = true)
@Composable
private fun MovimientoScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        val sampleMov = Movimiento(1, "SUPERMERCADO ÉXITO", 150000, "GASTO", "2026-09-15 14:30:00", categoria = "Supermercado")
        MovimientoScreen(
            baseCount = 1,
            conteosMap = mapOf("SUPERMERCADO" to 1),
            listaTotalFiltrada = listOf(sampleMov),
            sublistaCount = 1,
            displayItems = listOf(
                MovimientoListItem.Header("HOY", "1 mov."),
                MovimientoListItem.Item(sampleMov)
            ),
            mesTexto = "Septiembre",
            anioTexto = "2026",
            categoriaFiltro = "TODOS",
            searchQuery = "",
            esSaldoOculto = false,
            pasoPaginacion = 10,
            onOpenSelectorMes = {},
            onOpenSelectorAnio = {},
            onExportarReporte = {},
            onTogglePrivacidad = {},
            onSearchChange = {},
            onOpenFiltroAvanzado = {},
            onSelectCategoria = {},
            onCargarMas = {},
            onAgregarMovimiento = {},
            onEditarMovimiento = {},
            onEliminarMovimiento = {}
        )
    }
}
