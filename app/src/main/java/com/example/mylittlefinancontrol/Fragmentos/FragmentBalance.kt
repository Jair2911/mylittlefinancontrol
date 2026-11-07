package com.example.mylittlefinancontrol.Fragmentos

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.ui.components.DeudaCardItem
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*
import kotlin.math.max
import kotlin.math.pow

class FragmentBalance : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyLittleFinanControlTheme {
                    DeudasStatefulScreen()
                }
            }
        }
    }
}

@Composable
fun DeudasStatefulScreen() {
    val context = LocalContext.current
    val db = remember { NotasDatabaseHelper.getInstance(context) }
    val scope = rememberCoroutineScope()

    var listaDeudasTodas by remember { mutableStateOf<List<Deuda>>(emptyList()) }
    var filtroTipoSeleccionado by remember { mutableStateOf("Todas") }
    var esSaldoOculto by remember { mutableStateOf(false) }

    var showDialogDeuda by remember { mutableStateOf(false) }
    var deudaEnEdicion by remember { mutableStateOf<Deuda?>(null) }
    var deudaParaAbonar by remember { mutableStateOf<Deuda?>(null) }
    var deudaParaEliminar by remember { mutableStateOf<Deuda?>(null) }

    fun recargar() {
        scope.launch(Dispatchers.IO) {
            val lista = db.getAllDeudas()
            withContext(Dispatchers.Main) {
                listaDeudasTodas = lista
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

    val ordenadas = remember(listaDeudasTodas, filtroTipoSeleccionado) {
        val porTipo = if (filtroTipoSeleccionado == "Todas") {
            listaDeudasTodas
        } else {
            listaDeudasTodas.filter { it.tipo.contains(filtroTipoSeleccionado, ignoreCase = true) }
        }
        porTipo.sortedWith(
            compareBy<Deuda> { (it.totalDeuda - it.totalPagado) <= 0.0 }
                .thenBy { it.concepto }
        )
    }

    DeudasScreen(
        listaTodas = listaDeudasTodas,
        listaFiltrada = ordenadas,
        filtroSeleccionado = filtroTipoSeleccionado,
        esSaldoOculto = esSaldoOculto,
        onTogglePrivacidad = { esSaldoOculto = !esSaldoOculto },
        onSelectFiltro = { filtroTipoSeleccionado = it },
        onAgregarDeuda = {
            deudaEnEdicion = null
            showDialogDeuda = true
        },
        onEditarDeuda = { deuda ->
            if ((deuda.totalDeuda - deuda.totalPagado) <= 0.0) {
                Toast.makeText(context, "Las deudas completadas no se pueden editar", Toast.LENGTH_SHORT).show()
            } else {
                deudaEnEdicion = deuda
                showDialogDeuda = true
            }
        },
        onEliminarDeuda = { deuda -> deudaParaEliminar = deuda },
        onAbonarDeuda = { deuda -> deudaParaAbonar = deuda }
    )

    if (showDialogDeuda) {
        DialogAgregarEditarDeuda(
            db = db,
            deudaExistente = deudaEnEdicion,
            onDismiss = { showDialogDeuda = false },
            onSave = { deuda, esEdicion, mantenerAbierto ->
                if (!esEdicion) {
                    db.insertDeuda(deuda)
                } else {
                    db.updateDeuda(deuda)
                }
                recargar()
                if (!mantenerAbierto) {
                    showDialogDeuda = false
                }
            }
        )
    }

    deudaParaAbonar?.let { deuda ->
        DialogAbonarDeuda(
            deuda = deuda,
            onDismiss = { deudaParaAbonar = null },
            onRegistrarAbono = { abono, mantenerAbierto ->
                val fecha = Constantes.formatoFecha(System.currentTimeMillis())
                val mov = Movimiento(
                    0,
                    "PAGO DEUDA: ${deuda.concepto.uppercase()}",
                    abono.toInt(),
                    "PAGO DEUDA",
                    fecha,
                    categoria = "Pago Deuda"
                )
                db.insertarAbonoDeudaTransaccional(mov, deuda.id)
                Toast.makeText(context, "Abono registrado con éxito", Toast.LENGTH_SHORT).show()
                recargar()
                if (!mantenerAbierto) {
                    deudaParaAbonar = null
                }
            }
        )
    }

    deudaParaEliminar?.let { deuda ->
        AlertDialog(
            onDismissRequest = { deudaParaEliminar = null },
            title = { Text("Eliminar Deuda", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar este registro?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.deleteDeuda(deuda.id)
                        deudaParaEliminar = null
                        recargar()
                    }
                ) {
                    Text("Eliminar", color = DabaTheme.colors.expenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deudaParaEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DeudasScreen(
    listaTodas: List<Deuda>,
    listaFiltrada: List<Deuda>,
    filtroSeleccionado: String,
    esSaldoOculto: Boolean,
    onTogglePrivacidad: () -> Unit,
    onSelectFiltro: (String) -> Unit,
    onAgregarDeuda: () -> Unit,
    onEditarDeuda: (Deuda) -> Unit,
    onEliminarDeuda: (Deuda) -> Unit,
    onAbonarDeuda: (Deuda) -> Unit
) {
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val deudasPendientes = listaTodas.filter { (it.totalDeuda - it.totalPagado) > 0.0 }
    val totalDeudaSum = deudasPendientes.sumOf { it.totalDeuda }
    val totalPagadoSum = deudasPendientes.sumOf { it.totalPagado }
    val totalRestante = deudasPendientes.sumOf { (it.totalDeuda - it.totalPagado).coerceAtLeast(0.0) }
    val progreso = if (totalDeudaSum > 0) ((totalPagadoSum / totalDeudaSum) * 100).toInt().coerceIn(0, 100) else 0

    val countTarjetas = listaTodas.count { it.tipo.contains("tarjeta", true) }
    val countPrestamos = listaTodas.count { it.tipo.contains("préstamo", true) || it.tipo.contains("prestamo", true) || it.tipo.contains("bancario", true) }
    val countPersonales = listaTodas.count { it.tipo.contains("personal", true) }

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
            // Hero Card Total Deudas
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
                                text = "SALDO PENDIENTE TOTAL",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.debtAmberBg
                            ) {
                                Text(
                                    text = "${deudasPendientes.size} pendientes",
                                    color = colors.debtAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (esSaldoOculto) "$ ••••••••" else format.format(totalRestante),
                                color = colors.textPrimary,
                                fontSize = 28.sp,
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
                                text = "Abonado: ${format.format(totalPagadoSum)}",
                                color = colors.primaryEmerald,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pendiente: ${format.format(totalRestante)}",
                                color = colors.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            // Chips de Filtro
            item {
                val chips = listOf(
                    "Todas" to "Todas (${listaTodas.size})",
                    "Tarjeta" to "Tarjetas ($countTarjetas)",
                    "Préstamo" to "Préstamos ($countPrestamos)",
                    "Personal" to "Personales ($countPersonales)"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { (key, label) ->
                        val selected = filtroSeleccionado == key
                        Surface(
                            modifier = Modifier.clickable { onSelectFiltro(key) },
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

            items(listaFiltrada, key = { it.id.ifEmpty { it.concepto } }) { deuda ->
                DeudaCardItem(
                    deuda = deuda,
                    onEditarClick = { onEditarDeuda(deuda) },
                    onEliminarClick = { onEliminarDeuda(deuda) },
                    onAbonarClick = { onAbonarDeuda(deuda) }
                )
            }
        }

        FloatingActionButton(
            onClick = onAgregarDeuda,
            containerColor = colors.primaryEmerald,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Agregar Deuda"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogAgregarEditarDeuda(
    db: NotasDatabaseHelper,
    deudaExistente: Deuda?,
    onDismiss: () -> Unit,
    onSave: (Deuda, esEdicion: Boolean, mantenerAbierto: Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val listaTiposDeuda = remember { db.obtenerCategoriasPorModulo("DEUDA") }
    var tipoSeleccionado by remember {
        mutableStateOf(deudaExistente?.tipo ?: listaTiposDeuda.firstOrNull() ?: "Tarjeta de Crédito")
    }
    var expandedTipo by remember { mutableStateOf(false) }
    var nuevoTipoInput by remember { mutableStateOf("") }

    var concepto by remember { mutableStateOf(deudaExistente?.concepto ?: "") }
    var descripcion by remember { mutableStateOf(deudaExistente?.descripcion ?: "") }
    var totalDeudaStr by remember {
        mutableStateOf(if (deudaExistente != null) deudaExistente.totalDeuda.toLong().toString() else "")
    }
    var totalPagadoStr by remember {
        mutableStateOf(if (deudaExistente != null) deudaExistente.totalPagado.toLong().toString() else "0")
    }
    var tasaInteresStr by remember {
        mutableStateOf(if (deudaExistente != null && deudaExistente.tasaInteres > 0) deudaExistente.tasaInteres.toString() else "")
    }
    var tipoInteres by remember {
        mutableStateOf(if (!deudaExistente?.tipoInteres.isNullOrBlank()) deudaExistente.tipoInteres else "Anual")
    }
    var cuotasSeleccionadas by remember {
        mutableIntStateOf(if (deudaExistente != null && deudaExistente.cuotas > 0) deudaExistente.cuotas else 12)
    }
    var cuotasOtroStr by remember { mutableStateOf("") }
    var valorCuotaManualStr by remember {
        mutableStateOf(if (deudaExistente != null && deudaExistente.valorCuota > 0) deudaExistente.valorCuota.toLong().toString() else "")
    }
    var fechaInicio by remember { mutableStateOf(deudaExistente?.fechaInicio ?: "") }
    var fechaFin by remember { mutableStateOf(deudaExistente?.fechaFin ?: "") }
    var showConfirmAnother by remember { mutableStateOf(false) }

    val capitalBase = Constantes.limpiarYParsearMonto(totalDeudaStr)
    val pagado = Constantes.limpiarYParsearMonto(totalPagadoStr)
    val tasaNum = tasaInteresStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val cuotaManual = Constantes.limpiarYParsearMonto(valorCuotaManualStr)
    val n = if (cuotasSeleccionadas > 0) cuotasSeleccionadas else 1

    val (valorCuotaCalculada, totalDeudaRealCalculado) = remember(
        capitalBase, pagado, tasaNum, cuotaManual, n, tipoInteres
    ) {
        if (cuotaManual > 0.0) {
            val vc = Constantes.redondearMonto(cuotaManual)
            vc to Constantes.redondearMonto(vc * n)
        } else if (tasaNum > 0.0 && capitalBase > 0.0) {
            val rateMonthly = if (tipoInteres == "Anual") (tasaNum / 1200.0) else (tasaNum / 100.0)
            val vc = if (rateMonthly > 0) {
                val factor = (1.0 + rateMonthly).pow(n.toDouble())
                Constantes.redondearMonto(capitalBase * (rateMonthly * factor) / (factor - 1.0))
            } else {
                Constantes.redondearMonto(capitalBase / n)
            }
            vc to Constantes.redondearMonto(vc * n)
        } else {
            val vc = if (capitalBase > 0) Constantes.redondearMonto(capitalBase / n) else 0.0
            vc to Constantes.redondearMonto(capitalBase)
        }
    }

    val interesTotal = max(0.0, totalDeudaRealCalculado - capitalBase)
    val saldoRestante = max(0.0, totalDeudaRealCalculado - pagado)

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
                        text = if (deudaExistente != null) "Editar Deuda" else "Registrar Deuda",
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

                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Compromiso") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        listaTiposDeuda.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = {
                                    tipoSeleccionado = tipo
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }

                if (tipoSeleccionado == "Otra...") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nuevoTipoInput,
                        onValueChange = { nuevoTipoInput = it },
                        label = { Text("Nuevo tipo de compromiso") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto / Entidad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalDeudaStr,
                        onValueChange = { totalDeudaStr = it },
                        label = { Text("Capital ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = totalPagadoStr,
                        onValueChange = { totalPagadoStr = it },
                        label = { Text("Ya Abonado ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tasaInteresStr,
                        onValueChange = { tasaInteresStr = it },
                        label = { Text("Tasa Interés (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Surface(
                        modifier = Modifier.clickable {
                            tipoInteres = if (tipoInteres == "Anual") "Mensual" else "Anual"
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceContainerLow,
                        border = BorderStroke(1.dp, colors.primaryEmerald)
                    ) {
                        Text(
                            text = "% $tipoInteres",
                            color = colors.primaryEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Plazo en cuotas",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(6, 12, 24).forEach { c ->
                        val active = cuotasSeleccionadas == c
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    cuotasSeleccionadas = c
                                    cuotasOtroStr = ""
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) colors.surfaceContainerLow else colors.surfaceBg,
                            border = BorderStroke(1.dp, if (active) colors.primaryEmerald else colors.borderSubtle)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${c}x",
                                    color = if (active) colors.primaryEmerald else colors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = cuotasOtroStr,
                        onValueChange = {
                            cuotasOtroStr = it
                            val num = it.toIntOrNull()
                            if (num != null && num > 0) cuotasSeleccionadas = num
                        },
                        placeholder = { Text("Otro", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = valorCuotaManualStr,
                    onValueChange = { valorCuotaManualStr = it },
                    label = { Text("Cuota fija mensual manual (Opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fechaInicio,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Inicio") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> fechaInicio = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) },
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
                    OutlinedTextField(
                        value = fechaFin,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Vencimiento") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> fechaFin = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) },
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Resumen Calculado
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceBg,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Cuota est.: ${format.format(valorCuotaCalculada)} / mes",
                            color = colors.primaryEmerald,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Saldo Restante: ${format.format(saldoRestante)}",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Interés estimado: ${format.format(interesTotal)} (${String.format(Locale.US, "%.2f", tasaNum)}% $tipoInteres)",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Total real a pagar: ${format.format(totalDeudaRealCalculado)}",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
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
                            var tipoFinal = tipoSeleccionado
                            if (tipoSeleccionado == "Otra...") {
                                if (nuevoTipoInput.isBlank()) {
                                    Toast.makeText(context, "Escribe el nuevo tipo de compromiso", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                db.insertarCategoria(nuevoTipoInput.trim(), "DEUDA")
                                tipoFinal = nuevoTipoInput.trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            }
                            val totalFinal = if (totalDeudaRealCalculado > 0.0) totalDeudaRealCalculado else capitalBase
                            if (concepto.isNotBlank() && totalFinal > 0) {
                                if (deudaExistente == null) {
                                    onSave(
                                        Deuda(
                                            "",
                                            tipoFinal,
                                            concepto.trim(),
                                            descripcion.trim(),
                                            totalFinal,
                                            pagado,
                                            fechaInicio,
                                            fechaFin,
                                            tasaNum,
                                            tipoInteres,
                                            cuotasSeleccionadas,
                                            valorCuotaCalculada
                                        ),
                                        false,
                                        true
                                    )
                                    showConfirmAnother = true
                                } else {
                                    deudaExistente.tipo = tipoFinal
                                    deudaExistente.concepto = concepto.trim()
                                    deudaExistente.descripcion = descripcion.trim()
                                    deudaExistente.totalDeuda = totalFinal
                                    deudaExistente.totalPagado = pagado
                                    deudaExistente.fechaInicio = fechaInicio
                                    deudaExistente.fechaFin = fechaFin
                                    deudaExistente.tasaInteres = tasaNum
                                    deudaExistente.tipoInteres = tipoInteres
                                    deudaExistente.cuotas = cuotasSeleccionadas
                                    deudaExistente.valorCuota = valorCuotaCalculada
                                    onSave(deudaExistente, true, false)
                                }
                            } else {
                                Toast.makeText(context, "Por favor ingrese el concepto y el monto total", Toast.LENGTH_SHORT).show()
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
            text = { Text("¿Quieres agregar otro compromiso financiero?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        concepto = ""
                        descripcion = ""
                        totalDeudaStr = ""
                        totalPagadoStr = "0"
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
private fun DialogAbonarDeuda(
    deuda: Deuda,
    onDismiss: () -> Unit,
    onRegistrarAbono: (Double, mantenerAbierto: Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val restante = (deuda.totalDeuda - deuda.totalPagado).coerceAtLeast(0.0)

    var montoAbonoStr by remember {
        mutableStateOf(if (deuda.valorCuota > 0) deuda.valorCuota.toLong().toString() else "")
    }
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
                Text(
                    text = "Abonar a ${deuda.concepto}",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Saldo restante actual: ${format.format(restante)}",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = montoAbonoStr,
                    onValueChange = { montoAbonoStr = it },
                    label = { Text("Monto del abono ($)") },
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
                            val abono = montoAbonoStr.toDoubleOrNull() ?: 0.0
                            if (abono > 0) {
                                onRegistrarAbono(abono, true)
                                showConfirmAnother = true
                            } else {
                                Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
                    ) {
                        Text("Registrar Abono", color = Color.White, fontWeight = FontWeight.Bold)
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
            text = { Text("¿Quieres agregar otro registro a esta u otra deuda?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        montoAbonoStr = ""
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
private fun DeudasScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        val sample = listOf(
            Deuda("1", "Tarjeta de Crédito", "Visa NuBank", "Compras", 2400000.0, 800000.0, "2026-01-01", "2026-12-31", 24.5, "Anual", 12, 200000.0)
        )
        DeudasScreen(
            listaTodas = sample,
            listaFiltrada = sample,
            filtroSeleccionado = "Todas",
            esSaldoOculto = false,
            onTogglePrivacidad = {},
            onSelectFiltro = {},
            onAgregarDeuda = {},
            onEditarDeuda = {},
            onEliminarDeuda = {},
            onAbonarDeuda = {}
        )
    }
}
