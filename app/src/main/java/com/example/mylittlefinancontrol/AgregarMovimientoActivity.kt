package com.example.mylittlefinancontrol

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgregarMovimientoActivity : AppCompatActivity() {

    private lateinit var db: NotasDatabaseHelper
    private lateinit var firebaseAuth: FirebaseAuth

    private val listaDeudas = mutableStateListOf<Deuda>()
    private val listaGastosFijos = mutableStateListOf<GastoFijo>()
    private val listaCategorias = mutableStateListOf<String>()
    private val listaMetodosPago = mutableStateListOf<String>()
    private var metodoPagoInicial by mutableStateOf("Efectivo")
    private var fechaSeleccionadaTimestamp by mutableLongStateOf(System.currentTimeMillis())
    private var fechaTexto by mutableStateOf("")

    private var duplicadoDetectado by mutableStateOf<Pair<String, () -> Unit>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = NotasDatabaseHelper.getInstance(this)
        firebaseAuth = FirebaseAuth.getInstance()

        val cal = Calendar.getInstance()
        fechaTexto = "Hoy, ${String.format(Locale.US, "%02d:%02d hrs", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}"

        cargarDatosFormulario()

        setContent {
            MyLittleFinanControlTheme {
                AgregarMovimientoScreen(
                    categorias = listaCategorias,
                    deudas = listaDeudas,
                    gastosFijos = listaGastosFijos,
                    metodosPago = listaMetodosPago,
                    metodoPagoPorDefecto = metodoPagoInicial,
                    fechaDisplay = fechaTexto,
                    onSeleccionarFecha = { abrirSelectorFechaHora() },
                    onEscanearClick = {
                        Toast.makeText(this, "Abre la cámara desde el inicio o movimientos", Toast.LENGTH_SHORT).show()
                    },
                    onCancelarClick = { finish() },
                    onGuardarClick = { formData ->
                        procesarGuardado(formData)
                    }
                )

                duplicadoDetectado?.let { (mensaje, onConfirmar) ->
                    AlertDialog(
                        onDismissRequest = { duplicadoDetectado = null },
                        title = { Text("Movimiento Existente Detectado") },
                        text = { Text(mensaje) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    duplicadoDetectado = null
                                    onConfirmar()
                                }
                            ) {
                                Text("Guardar de todos modos")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { duplicadoDetectado = null }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun abrirSelectorFechaHora() {
        val sdf = SimpleDateFormat("dd MMM, HH:mm 'hrs'", Locale("es", "ES"))
        val c = Calendar.getInstance().apply { timeInMillis = fechaSeleccionadaTimestamp }
        DatePickerDialog(this, { _, y, m, d ->
            c.set(Calendar.YEAR, y)
            c.set(Calendar.MONTH, m)
            c.set(Calendar.DAY_OF_MONTH, d)
            TimePickerDialog(this, { _, hour, min ->
                c.set(Calendar.HOUR_OF_DAY, hour)
                c.set(Calendar.MINUTE, min)
                fechaSeleccionadaTimestamp = c.timeInMillis
                fechaTexto = sdf.format(c.time)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun cargarDatosFormulario() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cats = db.obtenerCategoriasPorModulo("MOVIMIENTO")
            val deudasPendientes = db.getAllDeudas().filter { (it.totalDeuda - it.totalPagado) > 0.0 }
            val gfTodas = db.getAllGastosFijos()
            val metodosPagoList = db.obtenerMetodosPago()
            val metodoPorDefecto = metodosPagoList.find { it.esPredeterminado } ?: metodosPagoList.firstOrNull()

            withContext(Dispatchers.Main) {
                listaCategorias.clear()
                listaCategorias.addAll(cats)

                listaDeudas.clear()
                listaDeudas.addAll(deudasPendientes)

                listaGastosFijos.clear()
                listaGastosFijos.addAll(gfTodas)

                listaMetodosPago.clear()
                listaMetodosPago.addAll(metodosPagoList.map { it.nombreFormateado() })
                metodoPagoInicial = metodoPorDefecto?.nombreFormateado() ?: "Efectivo"
            }
        }
    }

    private fun procesarGuardado(data: AgregarMovimientoFormData) {
        val fecha = Constantes.formatoFecha(fechaSeleccionadaTimestamp)

        var categoriaFinal = data.categoria
        if (data.categoria == "Otra...") {
            val nuevaCatInput = data.nuevaCategoria.trim()
            if (nuevaCatInput.isBlank() || nuevaCatInput.equals("Otra...", ignoreCase = true)) {
                Toast.makeText(this, "Escribe un nombre de categoría válido", Toast.LENGTH_SHORT).show()
                return
            }
            db.insertarCategoria(nuevaCatInput, "MOVIMIENTO")
            categoriaFinal = nuevaCatInput.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        } else if (categoriaFinal.isBlank()) {
            categoriaFinal = "Varios"
        }

        val valor = data.valor.toIntOrNull() ?: 0
        val metodoPagoSel = data.metodoPago.ifBlank { "Efectivo" }

        val ejecutarGuardado: () -> Unit = {
            lifecycleScope.launch(Dispatchers.IO) {
                if (data.esPagoDeuda) {
                    val deuda = data.deudaSeleccionada
                    if (deuda == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AgregarMovimientoActivity, "Seleccione una deuda", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    deuda.totalPagado += valor
                    db.updateDeuda(deuda)
                    actualizarDeudaEnFirebase(deuda, valor)
                    guardarMovimento(
                        descripcion = data.descripcion.uppercase(),
                        valor = valor,
                        tipo_reg = "PAGO DEUDA",
                        fecha = fecha,
                        categoria = categoriaFinal,
                        metodoPago = metodoPagoSel
                    )
                } else if (data.esGastoFijo && data.gastoFijoSeleccionado != null) {
                    val gf = data.gastoFijoSeleccionado
                    val mesAnio = fecha.substring(0, 7)
                    gf.ultimoPagoQ1 = mesAnio
                    db.updateGastoFijo(gf)
                    guardarMovimento(
                        descripcion = data.descripcion.uppercase(),
                        valor = valor,
                        tipo_reg = "GASTO FIJO",
                        fecha = fecha,
                        gastoFijoId = gf.id.toIntOrNull(),
                        quincena = 0,
                        categoria = categoriaFinal,
                        metodoPago = metodoPagoSel
                    )
                } else {
                    guardarMovimento(
                        descripcion = data.descripcion.uppercase(),
                        valor = valor,
                        tipo_reg = data.tipoSeleccionado.uppercase(),
                        fecha = fecha,
                        categoria = categoriaFinal,
                        metodoPago = metodoPagoSel
                    )
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val duplicado = db.buscarMovimientoDuplicado(valor, fecha)
            withContext(Dispatchers.Main) {
                if (duplicado != null) {
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
                    duplicadoDetectado = mensaje to ejecutarGuardado
                } else {
                    ejecutarGuardado()
                }
            }
        }
    }

    private fun actualizarDeudaEnFirebase(deuda: Deuda, abono: Int) {
        val uid = firebaseAuth.uid ?: return
        val nuevoPagado = deuda.totalPagado + abono
        FirebaseDatabase.getInstance().getReference("Deudas")
            .child(uid)
            .child(deuda.id)
            .child("totalPagado")
            .setValue(nuevoPagado)
    }

    private fun guardarMovimento(
        descripcion: String,
        valor: Int,
        tipo_reg: String,
        fecha: String,
        gastoFijoId: Int? = null,
        quincena: Int? = null,
        categoria: String = "Varios",
        metodoPago: String = "Efectivo"
    ) {
        val fc = Constantes.formatoFecha(System.currentTimeMillis())
        val movimiento = Movimiento(
            id = 0,
            descripcion = descripcion,
            valor = valor,
            tipo_reg = tipo_reg,
            fecha = fecha,
            gastoFijoId = gastoFijoId,
            quincenaPagada = quincena,
            categoria = categoria,
            fechaCreacion = fc,
            metodoPago = metodoPago
        )
        db.insertNota(movimiento)
        runOnUiThread {
            Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

data class AgregarMovimientoFormData(
    val tipoSeleccionado: String,
    val descripcion: String,
    val valor: String,
    val categoria: String,
    val nuevaCategoria: String,
    val metodoPago: String,
    val esPagoDeuda: Boolean,
    val deudaSeleccionada: Deuda?,
    val esGastoFijo: Boolean,
    val gastoFijoSeleccionado: GastoFijo?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarMovimientoScreen(
    categorias: List<String>,
    deudas: List<Deuda>,
    gastosFijos: List<GastoFijo>,
    metodosPago: List<String>,
    metodoPagoPorDefecto: String,
    fechaDisplay: String,
    onSeleccionarFecha: () -> Unit,
    onEscanearClick: () -> Unit,
    onCancelarClick: () -> Unit,
    onGuardarClick: (AgregarMovimientoFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors

    var tipoSeleccionado by remember { mutableStateOf("GASTO") }
    var valor by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember(categorias) {
        mutableStateOf(categorias.firstOrNull() ?: "Varios")
    }
    var nuevaCategoria by remember { mutableStateOf("") }
    var metodoPagoSeleccionado by remember(metodoPagoPorDefecto) {
        mutableStateOf(metodoPagoPorDefecto)
    }

    var esPagoDeuda by remember { mutableStateOf(false) }
    var deudaSeleccionada by remember { mutableStateOf<Deuda?>(null) }

    var esGastoFijo by remember { mutableStateOf(false) }
    var gastoFijoSeleccionado by remember { mutableStateOf<GastoFijo?>(null) }

    var expCategorias by remember { mutableStateOf(false) }
    var expDeudas by remember { mutableStateOf(false) }
    var expGastosFijos by remember { mutableStateOf(false) }
    var expMetodosPago by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.surfaceCard,
        unfocusedContainerColor = colors.surfaceCard,
        focusedBorderColor = colors.primaryEmerald,
        unfocusedBorderColor = colors.borderSubtle
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 36.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Agregar Movimiento",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ingresa los detalles de la transacción",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Surface(
                color = colors.surfaceContainerLow,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onEscanearClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = null,
                        tint = colors.primaryEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Escanear",
                        color = colors.primaryEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Segmented Tabs
        Text(
            text = "TIPO DE REGISTRO",
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(colors.surfaceContainer)
                .padding(3.dp)
        ) {
            val tabs = listOf(
                Triple("GASTO", "Gasto", colors.expenseRed),
                Triple("INGRESO", "Ingreso", colors.primaryEmerald),
                Triple("TRANSFERENCIA", "Transferencia", colors.textPrimary)
            )

            tabs.forEach { (key, label, activeColor) ->
                val isSelected = tipoSeleccionado == key
                Surface(
                    color = if (isSelected) colors.surfaceCard else Color.Transparent,
                    shape = RoundedCornerShape(percent = 50),
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { tipoSeleccionado = key }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = if (isSelected) activeColor else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Valor del Movimiento (Hero Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Valor del Movimiento",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$",
                        color = colors.textMuted,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    BasicTextField(
                        value = valor,
                        onValueChange = { valor = it },
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (valor.isEmpty()) {
                                Text(
                                    text = "0.00",
                                    color = colors.textMuted,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Toggle 1: ¿Es un pago de deuda o préstamo?
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_credit_card),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "¿Es un pago de deuda o préstamo?",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                Switch(
                    checked = esPagoDeuda,
                    onCheckedChange = { checked ->
                        esPagoDeuda = checked
                        if (checked) {
                            esGastoFijo = false
                            gastoFijoSeleccionado = null
                        } else {
                            deudaSeleccionada = null
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.primaryEmerald,
                        checkedTrackColor = colors.primaryEmerald50
                    )
                )
            }
        }

        if (esPagoDeuda) {
            ExposedDropdownMenuBox(
                expanded = expDeudas,
                onExpandedChange = { expDeudas = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                OutlinedTextField(
                    value = deudaSeleccionada?.let { "${it.concepto} (Resta: $${it.totalDeuda - it.totalPagado})" }.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona la deuda") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expDeudas) },
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expDeudas,
                    onDismissRequest = { expDeudas = false }
                ) {
                    deudas.forEach { d ->
                        DropdownMenuItem(
                            text = { Text("${d.concepto} (Resta: $${d.totalDeuda - d.totalPagado})") },
                            onClick = {
                                deudaSeleccionada = d
                                descripcion = "PAGO DEUDA: ${d.concepto}"
                                expDeudas = false
                            }
                        )
                    }
                }
            }
        }

        // Toggle 2: ¿Es un pago de gasto fijo?
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_autorenew),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "¿Corresponde a un Gasto Fijo?",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                Switch(
                    checked = esGastoFijo,
                    onCheckedChange = { checked ->
                        esGastoFijo = checked
                        if (checked) {
                            esPagoDeuda = false
                            deudaSeleccionada = null
                        } else {
                            gastoFijoSeleccionado = null
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.primaryEmerald,
                        checkedTrackColor = colors.primaryEmerald50
                    )
                )
            }
        }

        if (esGastoFijo) {
            ExposedDropdownMenuBox(
                expanded = expGastosFijos,
                onExpandedChange = { expGastosFijos = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                OutlinedTextField(
                    value = gastoFijoSeleccionado?.let { "${it.concepto} ($${it.monto})" }.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona el gasto fijo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expGastosFijos) },
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expGastosFijos,
                    onDismissRequest = { expGastosFijos = false }
                ) {
                    gastosFijos.forEach { gf ->
                        DropdownMenuItem(
                            text = { Text("${gf.concepto} ($${gf.monto})") },
                            onClick = {
                                gastoFijoSeleccionado = gf
                                descripcion = "PAGO: ${gf.concepto}"
                                valor = gf.monto.toInt().toString()
                                expGastosFijos = false
                            }
                        )
                    }
                }
            }
        }

        // Descripción
        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción del movimiento") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(14.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // Categoría
        ExposedDropdownMenuBox(
            expanded = expCategorias,
            onExpandedChange = { expCategorias = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            OutlinedTextField(
                value = categoriaSeleccionada,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCategorias) },
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expCategorias,
                onDismissRequest = { expCategorias = false }
            ) {
                categorias.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            categoriaSeleccionada = cat
                            expCategorias = false
                        }
                    )
                }
            }
        }

        if (categoriaSeleccionada == "Otra...") {
            OutlinedTextField(
                value = nuevaCategoria,
                onValueChange = { nuevaCategoria = it },
                label = { Text("Nombre de la nueva categoría") },
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        // Método de Pago
        ExposedDropdownMenuBox(
            expanded = expMetodosPago,
            onExpandedChange = { expMetodosPago = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            OutlinedTextField(
                value = metodoPagoSeleccionado,
                onValueChange = {},
                readOnly = true,
                label = { Text("Método de pago") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expMetodosPago) },
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expMetodosPago,
                onDismissRequest = { expMetodosPago = false }
            ) {
                metodosPago.forEach { mp ->
                    DropdownMenuItem(
                        text = { Text(mp) },
                        onClick = {
                            metodoPagoSeleccionado = mp
                            expMetodosPago = false
                        }
                    )
                }
            }
        }

        // Fecha y Hora
        Surface(
            color = colors.surfaceCard,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onSeleccionarFecha)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_calendar_today),
                        contentDescription = null,
                        tint = colors.primaryEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Fecha y Hora",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = fechaDisplay,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Botones Guardar y Cancelar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelarClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancelar", color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onGuardarClick(
                        AgregarMovimientoFormData(
                            tipoSeleccionado = tipoSeleccionado,
                            descripcion = descripcion.trim(),
                            valor = valor.trim(),
                            categoria = categoriaSeleccionada,
                            nuevaCategoria = nuevaCategoria.trim(),
                            metodoPago = metodoPagoSeleccionado,
                            esPagoDeuda = esPagoDeuda,
                            deudaSeleccionada = deudaSeleccionada,
                            esGastoFijo = esGastoFijo,
                            gastoFijoSeleccionado = gastoFijoSeleccionado
                        )
                    )
                },
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryEmerald,
                    contentColor = Color.White
                )
            ) {
                Text("Guardar Movimiento", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AgregarMovimientoScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        AgregarMovimientoScreen(
            categorias = listOf("Supermercado", "Comida", "Transporte", "Otra..."),
            deudas = emptyList(),
            gastosFijos = emptyList(),
            metodosPago = listOf("Efectivo", "Tarjeta Débito"),
            metodoPagoPorDefecto = "Efectivo",
            fechaDisplay = "Hoy, 14:30 hrs",
            onSeleccionarFecha = {},
            onEscanearClick = {},
            onCancelarClick = {},
            onGuardarClick = {}
        )
    }
}
