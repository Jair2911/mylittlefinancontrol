package com.example.mylittlefinancontrol.Fragmentos

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.Modelos.Ahorro
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.ui.components.AhorroCardItem
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FragmentAhorros : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyLittleFinanControlTheme {
                    AhorrosStatefulScreen()
                }
            }
        }
    }
}

@Composable
fun AhorrosStatefulScreen() {
    val context = LocalContext.current
    val db = remember { NotasDatabaseHelper.getInstance(context) }
    val scope = rememberCoroutineScope()

    var listaAhorrosTodos by remember { mutableStateOf<List<Ahorro>>(emptyList()) }
    var categoriaFiltroSeleccionada by remember { mutableStateOf("Todos") }
    var filtroEstadoAhorros by remember { mutableStateOf("TODOS") }
    var criterioOrdenAhorros by remember { mutableStateOf("DEFAULT") }
    var esOculto by remember { mutableStateOf(false) }

    var showDialogAhorro by remember { mutableStateOf(false) }
    var ahorroEnEdicion by remember { mutableStateOf<Ahorro?>(null) }
    var ahorroParaAbonar by remember { mutableStateOf<Ahorro?>(null) }
    var ahorroParaEliminar by remember { mutableStateOf<Ahorro?>(null) }
    var showFiltroDialog by remember { mutableStateOf(false) }

    fun recargar() {
        scope.launch(Dispatchers.IO) {
            val lista = db.getAllAhorros()
            withContext(Dispatchers.Main) {
                listaAhorrosTodos = lista
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

    val filtrados = remember(listaAhorrosTodos, categoriaFiltroSeleccionada, filtroEstadoAhorros, criterioOrdenAhorros) {
        var list = when (categoriaFiltroSeleccionada) {
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

        list = when (filtroEstadoAhorros) {
            "EN_PROGRESO" -> list.filter { it.montoActual < it.montoObjetivo }
            "COMPLETADAS" -> list.filter { it.montoActual >= it.montoObjetivo }
            else -> list
        }

        when (criterioOrdenAhorros) {
            "MAYOR_PROGRESO" -> list.sortedByDescending { if (it.montoObjetivo > 0) it.montoActual / it.montoObjetivo else 0.0 }
            "MENOR_PROGRESO" -> list.sortedBy { if (it.montoObjetivo > 0) it.montoActual / it.montoObjetivo else 0.0 }
            "MAYOR_MONTO" -> list.sortedByDescending { it.montoObjetivo }
            "MENOR_MONTO" -> list.sortedBy { it.montoObjetivo }
            "ALFABETICO" -> list.sortedBy { it.titulo }
            else -> list
        }
    }

    AhorrosScreen(
        listaTodos = listaAhorrosTodos,
        listaFiltrada = filtrados,
        categoriaSeleccionada = categoriaFiltroSeleccionada,
        esOculto = esOculto,
        onTogglePrivacidad = { esOculto = !esOculto },
        onSelectCategoria = { categoriaFiltroSeleccionada = it },
        onOpenFilter = { showFiltroDialog = true },
        onAgregarMeta = {
            ahorroEnEdicion = null
            showDialogAhorro = true
        },
        onEditarMeta = { ahorro ->
            ahorroEnEdicion = ahorro
            showDialogAhorro = true
        },
        onEliminarMeta = { ahorro -> ahorroParaEliminar = ahorro },
        onAbonarMeta = { ahorro -> ahorroParaAbonar = ahorro }
    )

    if (showFiltroDialog) {
        val opciones = listOf(
            "Todas las metas (Por defecto)",
            "En progreso (< 100%)",
            "Metas cumplidas (100%)",
            "Ordenar: Mayor % de avance",
            "Ordenar: Menor % de avance",
            "Ordenar: Mayor meta de ahorro ($$$)",
            "Ordenar: Menor meta de ahorro ($)",
            "Ordenar: Alfabéticamente (A-Z)"
        )
        AlertDialog(
            onDismissRequest = { showFiltroDialog = false },
            title = { Text("Filtrar y Ordenar Metas", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    opciones.forEachIndexed { which, label ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
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

    if (showDialogAhorro) {
        DialogAgregarEditarAhorro(
            ahorroExistente = ahorroEnEdicion,
            onDismiss = { showDialogAhorro = false },
            onSave = { nuevoAhorro, esEdicion, mantenerAbierto ->
                if (!esEdicion) {
                    db.insertAhorro(nuevoAhorro)
                } else {
                    db.updateAhorro(nuevoAhorro)
                }
                recargar()
                if (!mantenerAbierto) {
                    showDialogAhorro = false
                }
            }
        )
    }

    ahorroParaAbonar?.let { ahorro ->
        DialogAbonarAhorro(
            ahorro = ahorro,
            onDismiss = { ahorroParaAbonar = null },
            onConfirmarAbono = { abono, mantenerAbierto ->
                val fecha = Constantes.formatoFecha(System.currentTimeMillis())
                val mov = Movimiento(
                    0,
                    "AHORRO: ${ahorro.titulo.uppercase()}",
                    abono.toInt(),
                    "AHORRO",
                    fecha,
                    categoria = "Ahorro"
                )
                val ahorroIdInt = ahorro.id.toIntOrNull() ?: -1
                if (ahorroIdInt != -1) {
                    db.insertarAbonoAhorroTransaccional(mov, ahorroIdInt)
                } else {
                    ahorro.montoActual += abono
                    db.updateAhorro(ahorro)
                    db.insertNota(mov)
                }
                recargar()
                val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                Toast.makeText(context, "Aporte de ${format.format(abono)} registrado con éxito", Toast.LENGTH_SHORT).show()
                if (!mantenerAbierto) {
                    ahorroParaAbonar = null
                }
            }
        )
    }

    ahorroParaEliminar?.let { ahorro ->
        AlertDialog(
            onDismissRequest = { ahorroParaEliminar = null },
            title = { Text("Eliminar Meta de Ahorro", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar '${ahorro.titulo}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.deleteAhorro(ahorro.id)
                        ahorroParaEliminar = null
                        recargar()
                        Toast.makeText(context, "Meta eliminada correctamente", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Eliminar", color = DabaTheme.colors.expenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ahorroParaEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AhorrosScreen(
    listaTodos: List<Ahorro>,
    listaFiltrada: List<Ahorro>,
    categoriaSeleccionada: String,
    esOculto: Boolean,
    onTogglePrivacidad: () -> Unit,
    onSelectCategoria: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAgregarMeta: () -> Unit,
    onEditarMeta: (Ahorro) -> Unit,
    onEliminarMeta: (Ahorro) -> Unit,
    onAbonarMeta: (Ahorro) -> Unit
) {
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val totalAhorrado = listaTodos.sumOf { it.montoActual }
    val totalMetaGlobal = listaTodos.sumOf { it.montoObjetivo }
    val faltante = (totalMetaGlobal - totalAhorrado).coerceAtLeast(0.0)
    val progresoGlobal = if (totalMetaGlobal > 0) ((totalAhorrado / totalMetaGlobal) * 100).toInt().coerceIn(0, 100) else 0

    val countEmergencia = listaTodos.count { it.categoria.contains("emerg", true) || it.categoria.contains("salud", true) }
    val countVehiculo = listaTodos.count { it.categoria.contains("veh", true) || it.categoria.contains("auto", true) || it.categoria.contains("carro", true) }
    val countViajes = listaTodos.count { it.categoria.contains("viaj", true) || it.categoria.contains("vaca", true) }
    val countVivienda = listaTodos.count { it.categoria.contains("viv", true) || it.categoria.contains("casa", true) || it.categoria.contains("hogar", true) }
    val countOtros = listaTodos.count {
        !it.categoria.contains("emerg", true) &&
            !it.categoria.contains("veh", true) &&
            !it.categoria.contains("viaj", true) &&
            !it.categoria.contains("viv", true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceBg)
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
                            text = "Metas de Ahorro",
                            color = colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${listaTodos.size} metas de ahorro activas",
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

            // Hero Card Total Ahorrado
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
                                text = "PATRIMONIO EN METAS",
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
                                    text = "${listaTodos.size} Metas Activas",
                                    color = colors.primaryEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (esOculto) "$ ••••••••" else format.format(totalAhorrado),
                            color = colors.primaryEmerald,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progresoGlobal / 100f },
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
                                text = if (esOculto) "Meta Global: $ ••••••••" else "Meta Global: ${format.format(totalMetaGlobal)}",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$progresoGlobal% Completado",
                                color = colors.primaryEmerald,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (esOculto) "Faltan: $ ••••••••" else "Faltan: ${format.format(faltante)}",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Filter Chips
            item {
                val chips = listOf(
                    "Todos" to "Todos (${listaTodos.size})",
                    "Emergencia" to "Emergencia ($countEmergencia)",
                    "Vehículo" to "Vehículo ($countVehiculo)",
                    "Viajes" to "Viajes ($countViajes)",
                    "Vivienda" to "Vivienda ($countVivienda)",
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

            // Section Title + Nueva Meta button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis Objetivos",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+ Nueva Meta",
                        color = colors.primaryEmerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onAgregarMeta)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (listaFiltrada.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_savings),
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tienes metas de ahorro en esta categoría",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pulsa el botón + para crear tu primera meta financiera",
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(listaFiltrada, key = { it.id.ifEmpty { it.titulo } }) { ahorro ->
                    AhorroCardItem(
                        ahorro = ahorro,
                        onEditarClick = { onEditarMeta(ahorro) },
                        onEliminarClick = { onEliminarMeta(ahorro) },
                        onAbonarClick = { onAbonarMeta(ahorro) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAgregarMeta,
            containerColor = colors.primaryEmerald,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Agregar Meta de Ahorro"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogAgregarEditarAhorro(
    ahorroExistente: Ahorro?,
    onDismiss: () -> Unit,
    onSave: (Ahorro, esEdicion: Boolean, mantenerAbierto: Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = DabaTheme.colors

    var titulo by remember { mutableStateOf(ahorroExistente?.titulo ?: "") }
    var montoObjetivoStr by remember {
        mutableStateOf(
            if (ahorroExistente != null) String.format(Locale.US, "%.0f", ahorroExistente.montoObjetivo) else ""
        )
    }
    var montoActualStr by remember {
        mutableStateOf(
            if (ahorroExistente != null) String.format(Locale.US, "%.0f", ahorroExistente.montoActual) else ""
        )
    }
    var fechaMeta by remember { mutableStateOf(ahorroExistente?.fechaMeta ?: "") }
    var categoriaSeleccionada by remember { mutableStateOf(ahorroExistente?.categoria ?: "Emergencia") }
    val ubicaciones = remember { listOf("Cuenta Bancaria / Bolsillo", "Alcancía / Efectivo", "Inversión / CDT") }
    var ubicacionSeleccionada by remember { mutableStateOf(ahorroExistente?.ubicacion ?: ubicaciones[0]) }
    var expandedUbicacion by remember { mutableStateOf(false) }
    var showConfirmAnother by remember { mutableStateOf(false) }

    val categoriasGrid = remember {
        listOf(
            Triple("Emergencia", R.drawable.ic_shield, "Emergencia"),
            Triple("Vehículo", R.drawable.ic_directions_car, "Vehículo"),
            Triple("Viajes", R.drawable.ic_cloud, "Viajes"),
            Triple("Vivienda", R.drawable.ic_cottage, "Vivienda"),
            Triple("Tecnología", R.drawable.ic_router, "Tecnología"),
            Triple("Otro", R.drawable.ic_savings, "Otro")
        )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (ahorroExistente != null) "Editar Meta de Ahorro" else "Nueva Meta de Ahorro",
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CATEGORÍA",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 3x2 Category Grid
                for (row in categoriasGrid.chunked(3)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((catName, iconRes, label) in row) {
                            val active = categoriaSeleccionada.equals(catName, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { categoriaSeleccionada = catName },
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) colors.surfaceContainerLow else colors.surfaceBg,
                                border = BorderStroke(
                                    if (active) 1.5.dp else 1.dp,
                                    if (active) colors.primaryEmerald else colors.borderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = label,
                                        tint = if (active) colors.primaryEmerald else colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        color = if (active) colors.primaryEmerald else colors.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Nombre de la meta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = montoObjetivoStr,
                        onValueChange = { montoObjetivoStr = it },
                        label = { Text("Meta ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = montoActualStr,
                        onValueChange = { montoActualStr = it },
                        label = { Text("Ahorrado ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fechaMeta,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha objetivo (Opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val c = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                    val sdf = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                    fechaMeta = sdf.format(c.time).replaceFirstChar { it.uppercase() }
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = colors.textPrimary,
                        disabledBorderColor = colors.borderSubtle,
                        disabledLabelColor = colors.textSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedUbicacion,
                    onExpandedChange = { expandedUbicacion = !expandedUbicacion }
                ) {
                    OutlinedTextField(
                        value = ubicacionSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("¿Dónde está guardado?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUbicacion) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedUbicacion,
                        onDismissRequest = { expandedUbicacion = false }
                    ) {
                        ubicaciones.forEach { ub ->
                            DropdownMenuItem(
                                text = { Text(ub) },
                                onClick = {
                                    ubicacionSeleccionada = ub
                                    expandedUbicacion = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                            val montoObj = montoObjetivoStr.replace("$", "").replace(".", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                            val montoAct = montoActualStr.replace("$", "").replace(".", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                            if (titulo.isNotBlank() && montoObj > 0) {
                                if (ahorroExistente == null) {
                                    onSave(
                                        Ahorro(
                                            "",
                                            titulo.trim(),
                                            categoriaSeleccionada,
                                            montoAct,
                                            montoObj,
                                            fechaMeta,
                                            ubicacion = ubicacionSeleccionada
                                        ),
                                        false,
                                        true
                                    )
                                    showConfirmAnother = true
                                } else {
                                    ahorroExistente.titulo = titulo.trim()
                                    ahorroExistente.montoObjetivo = montoObj
                                    ahorroExistente.montoActual = montoAct
                                    ahorroExistente.fechaMeta = fechaMeta
                                    ahorroExistente.categoria = categoriaSeleccionada
                                    ahorroExistente.ubicacion = ubicacionSeleccionada
                                    onSave(ahorroExistente, true, false)
                                }
                            } else {
                                Toast.makeText(context, "Por favor ingrese el nombre de la meta y un monto objetivo válido", Toast.LENGTH_SHORT).show()
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
            text = { Text("¿Quieres agregar otra meta o fondo de ahorro?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        titulo = ""
                        montoObjetivoStr = ""
                        montoActualStr = ""
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
private fun DialogAbonarAhorro(
    ahorro: Ahorro,
    onDismiss: () -> Unit,
    onConfirmarAbono: (Double, mantenerAbierto: Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    var montoAbonoStr by remember { mutableStateOf("") }
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
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Realizar Aporte",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ahorro.titulo,
                            color = colors.primaryEmerald,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
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

                Text(
                    text = "Progreso actual: ${format.format(ahorro.montoActual)} de ${format.format(ahorro.montoObjetivo)}",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = montoAbonoStr,
                    onValueChange = { montoAbonoStr = it },
                    label = { Text("Monto a abonar ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            val abono = montoAbonoStr.replace("$", "").replace(".", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                            if (abono > 0) {
                                onConfirmarAbono(abono, true)
                                showConfirmAnother = true
                            } else {
                                Toast.makeText(context, "Por favor ingrese un monto de aporte válido", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
                    ) {
                        Text("Abonar", color = Color.White, fontWeight = FontWeight.Bold)
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
            title = { Text("¡Aporte exitoso!", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas realizar otro aporte a esta u otra meta?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        montoAbonoStr = ""
                        showConfirmAnother = false
                    }
                ) {
                    Text("Sí, realizar otro", color = colors.primaryEmerald, fontWeight = FontWeight.Bold)
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
private fun AhorrosScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        val sample = listOf(
            Ahorro("1", "Fondo de Emergencia", "Emergencia", 1500000.0, 3000000.0, "Dic 2026")
        )
        AhorrosScreen(
            listaTodos = sample,
            listaFiltrada = sample,
            categoriaSeleccionada = "Todos",
            esOculto = false,
            onTogglePrivacidad = {},
            onSelectCategoria = {},
            onOpenFilter = {},
            onAgregarMeta = {},
            onEditarMeta = {},
            onEliminarMeta = {},
            onAbonarMeta = {}
        )
    }
}
