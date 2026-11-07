package com.example.mylittlefinancontrol

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.Modelos.CategoriaGastoItem
import com.example.mylittlefinancontrol.Modelos.ResumenPeriodoData
import com.example.mylittlefinancontrol.Servicios.AnalizadorFinancieroLocal
import com.example.mylittlefinancontrol.Servicios.DiagnosticoFinanciero
import com.example.mylittlefinancontrol.ui.components.CategoriaResumenItem
import com.example.mylittlefinancontrol.ui.components.MovimientoHeaderItem
import com.example.mylittlefinancontrol.ui.components.MovimientoItem
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetallePeriodoActivity : AppCompatActivity() {

    private lateinit var db: NotasDatabaseHelper

    private var tipoPeriodo: String = "MENSUAL"
    private var tituloPeriodo by mutableStateOf("Resumen Mensual")
    private var startDate by mutableStateOf("")
    private var endDate by mutableStateOf("")
    private var prevStartDate: String = ""
    private var prevEndDate: String = ""

    private var resumenActual by mutableStateOf<ResumenPeriodoData?>(null)
    private var listaMovimientosPeriodo by mutableStateOf<List<Movimiento>>(emptyList())
    private var diagnosticoActual by mutableStateOf<DiagnosticoFinanciero?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = NotasDatabaseHelper.getInstance(this)
        tipoPeriodo = intent.getStringExtra("TIPO_PERIODO") ?: "MENSUAL"

        calcularRangosFechas()
        cargarDatosPeriodo()

        setContent {
            MyLittleFinanControlTheme {
                var mostrarDialogoDiagnostico by remember { mutableStateOf(false) }
                var categoriaSeleccionadaBs by remember { mutableStateOf<CategoriaGastoItem?>(null) }

                DetallePeriodoScreen(
                    tituloPeriodo = tituloPeriodo,
                    rangoFechas = "📅 $startDate  a  $endDate",
                    resumen = resumenActual,
                    diagnostico = diagnosticoActual,
                    onBackClick = { finish() },
                    onCategoriaClick = { item -> categoriaSeleccionadaBs = item },
                    onDiagnosticoClick = {
                        if (resumenActual?.totalIngresos == 0.0 && resumenActual?.totalGastos == 0.0) {
                            Toast.makeText(
                                this,
                                "No hay suficiente información para analizar en este periodo",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            mostrarDialogoDiagnostico = true
                        }
                    },
                    onExportarPdfClick = { exportarReportePDF() },
                    onExportarCsvClick = { exportarReporteCSV() }
                )

                if (mostrarDialogoDiagnostico && diagnosticoActual != null) {
                    val diag = diagnosticoActual!!
                    val iconoEstado = when (diag.estadoGeneral) {
                        "Excelente", "Saludable" -> "🟢"
                        "En Riesgo" -> "🟡"
                        else -> "🔴"
                    }
                    val mensajeDetallado = buildString {
                        append("$iconoEstado Estado: ${diag.estadoGeneral.uppercase()} (Salud: ${diag.puntajeSalud}/100)\n\n")
                        append("📌 Resumen Ejecutivo:\n${diag.resumenEjecutivo}\n\n")
                        append("🔍 Puntos Clave:\n")
                        diag.puntosClave.forEach { p -> append("• $p\n") }
                        append("\n💡 Recomendación Accionable:\n${diag.recomendacionAccionable}")
                    }

                    AlertDialog(
                        onDismissRequest = { mostrarDialogoDiagnostico = false },
                        title = { Text("📊 Diagnóstico Financiero ($tipoPeriodo)") },
                        text = { Text(mensajeDetallado) },
                        confirmButton = {
                            Button(onClick = { mostrarDialogoDiagnostico = false }) {
                                Text("Entendido")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    mostrarDialogoDiagnostico = false
                                    exportarReportePDF()
                                }
                            ) {
                                Text("Exportar PDF")
                            }
                        }
                    )
                }

                categoriaSeleccionadaBs?.let { catItem ->
                    DetalleCategoriaBottomSheet(
                        item = catItem,
                        movimientos = listaMovimientosPeriodo.filter { mov ->
                            val cat = mov.categoria.ifBlank { "Varios" }
                            cat.equals(catItem.nombreCategoria, ignoreCase = true) &&
                                !mov.tipo_reg.contains("INGRESO", ignoreCase = true)
                        },
                        onDismiss = { categoriaSeleccionadaBs = null }
                    )
                }
            }
        }
    }

    private fun calcularRangosFechas() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        endDate = sdf.format(cal.time)

        when (tipoPeriodo) {
            "SEMANAL" -> {
                val calStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                startDate = sdf.format(calStart.time)

                val calPrevEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                val calPrevStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -13) }
                prevEndDate = sdf.format(calPrevEnd.time)
                prevStartDate = sdf.format(calPrevStart.time)

                tituloPeriodo = "Resumen Semanal"
            }
            "MENSUAL" -> {
                val calStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                startDate = sdf.format(calStart.time)

                val calPrevStart = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val calPrevEnd = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                prevStartDate = sdf.format(calPrevStart.time)
                prevEndDate = sdf.format(calPrevEnd.time)

                tituloPeriodo = "Resumen Mensual"
            }
            "SEMESTRAL" -> {
                val calStart = Calendar.getInstance().apply { add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }
                startDate = sdf.format(calStart.time)

                val calPrevStart = Calendar.getInstance().apply { add(Calendar.MONTH, -11); set(Calendar.DAY_OF_MONTH, 1) }
                val calPrevEnd = Calendar.getInstance().apply { add(Calendar.MONTH, -6); set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
                prevStartDate = sdf.format(calPrevStart.time)
                prevEndDate = sdf.format(calPrevEnd.time)

                tituloPeriodo = "Resumen Semestral"
            }
            else -> {
                val calStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }
                startDate = sdf.format(calStart.time)

                val calPrevStart = Calendar.getInstance().apply { add(Calendar.YEAR, -1); set(Calendar.DAY_OF_YEAR, 1) }
                val calPrevEnd = Calendar.getInstance().apply { add(Calendar.YEAR, -1); set(Calendar.MONTH, Calendar.DECEMBER); set(Calendar.DAY_OF_MONTH, 31) }
                prevStartDate = sdf.format(calPrevStart.time)
                prevEndDate = sdf.format(calPrevEnd.time)

                tituloPeriodo = "Resumen Anual"
            }
        }
    }

    private fun cargarDatosPeriodo() {
        lifecycleScope.launch(Dispatchers.IO) {
            val res = db.getResumenPorRangoFechas(startDate, endDate)
            val movs = db.getMovimientosPorRangoFechas(startDate, endDate)
            val gastosAnteriores = db.getTotalGastosPeriodoSQL(prevStartDate, prevEndDate)
            val totalGF = db.getAllGastosFijos().sumOf { it.monto }
            val totalDeudas = db.getAllDeudas().filter { (it.totalDeuda - it.totalPagado) > 0 }.sumOf { it.valorCuota }

            val diagnostico = withContext(Dispatchers.Default) {
                AnalizadorFinancieroLocal.generarDiagnostico(
                    periodo = tipoPeriodo,
                    ingresosTotales = res.totalIngresos,
                    gastosTotales = res.totalGastos,
                    gastosPeriodoAnterior = gastosAnteriores,
                    categorias = res.listaCategorias,
                    totalGastosFijos = totalGF,
                    totalCuotasDeudas = totalDeudas
                )
            }

            withContext(Dispatchers.Main) {
                resumenActual = res
                listaMovimientosPeriodo = movs
                diagnosticoActual = diagnostico
            }
        }
    }

    private fun exportarReportePDF() {
        val res = resumenActual ?: return
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = android.graphics.Color.parseColor("#0F382C")
            textSize = 20f
            isFakeBoldText = true
        }

        val paintSub = Paint().apply {
            color = android.graphics.Color.parseColor("#4F6354")
            textSize = 12f
        }

        val paintText = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
        }

        var y = 40f
        canvas.drawText("DABAFINN - Reporte Financiero ($tipoPeriodo)", 40f, y, paintTitle)
        y += 20f
        canvas.drawText("Rango: $startDate a $endDate | Generado: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}", 40f, y, paintSub)

        y += 35f
        canvas.drawText("--------------------------------------------------------------------------------", 40f, y, paintSub)
        y += 25f

        canvas.drawText("Ingresos Totales: ${format.format(res.totalIngresos)}", 40f, y, paintText)
        y += 20f
        canvas.drawText("Gastos Totales: ${format.format(res.totalGastos)}", 40f, y, paintText)
        y += 20f
        canvas.drawText("Balance Neto: ${format.format(res.balanceNeto)}", 40f, y, paintText)

        y += 35f
        canvas.drawText("DESGLOSE POR CATEGORÍAS (PATRONES Y TENDENCIAS)", 40f, y, paintTitle)
        y += 25f

        for (c in res.listaCategorias.take(10)) {
            val linea = "• ${c.nombreCategoria}: ${format.format(c.totalMonto)} (${c.porcentaje}% · ${c.conteoTransacciones} movs)"
            canvas.drawText(linea, 40f, y, paintText)
            y += 18f
        }

        val diag = diagnosticoActual
        if (diag != null) {
            y += 25f
            canvas.drawText("DIAGNÓSTICO FINANCIERO (${diag.estadoGeneral.uppercase()} - ${diag.puntajeSalud}/100)", 40f, y, paintTitle)
            y += 18f

            val lineasResumen = dividirTextoParaPdf(diag.resumenEjecutivo, 75)
            for (l in lineasResumen) {
                if (y > 780f) break
                canvas.drawText(l, 40f, y, paintText)
                y += 16f
            }

            y += 6f
            canvas.drawText("PUNTOS CLAVE Y HALLAZGOS:", 40f, y, paintSub)
            y += 16f

            for (p in diag.puntosClave) {
                if (y > 790f) break
                val lineasPunto = dividirTextoParaPdf("• $p", 75)
                for (lp in lineasPunto) {
                    if (y > 790f) break
                    canvas.drawText(lp, 40f, y, paintSub)
                    y += 15f
                }
            }

            if (y <= 790f) {
                y += 6f
                val lineasRec = dividirTextoParaPdf("💡 Recomendación: ${diag.recomendacionAccionable}", 75)
                for (lr in lineasRec) {
                    if (y > 805f) break
                    canvas.drawText(lr, 40f, y, paintText)
                    y += 15f
                }
            }
        }

        pdfDocument.finishPage(page)

        try {
            val file = File(getExternalFilesDir(null), "Reporte_DABAFINN_${tipoPeriodo}_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir Reporte PDF DABAFINN"))
        } catch (e: Exception) {
            Log.e("PDFExport", "Error creando PDF: ${e.message}", e)
            Toast.makeText(this, "Error al generar archivo PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportarReporteCSV() {
        val movs = listaMovimientosPeriodo

        val csvBuilder = StringBuilder()
        csvBuilder.append("Fecha,Concepto,Tipo,Categoria,Valor\n")

        for (m in movs) {
            val fecha = m.fecha.ifBlank { m.fechaCreacion }
            val desc = m.descripcion.replace(",", " ")
            val tipo = m.tipo_reg
            val cat = m.categoria.ifBlank { "Varios" }
            val valor = m.valor
            csvBuilder.append("$fecha,$desc,$tipo,$cat,$valor\n")
        }

        try {
            val file = File(getExternalFilesDir(null), "Movimientos_${tipoPeriodo}_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(file)
            fos.write(csvBuilder.toString().toByteArray())
            fos.close()

            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir Reporte CSV DABAFINN"))
        } catch (e: Exception) {
            Log.e("CSVExport", "Error creando CSV: ${e.message}", e)
            Toast.makeText(this, "Error al generar archivo CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dividirTextoParaPdf(texto: String, maxChars: Int = 75): List<String> {
        val palabras = texto.split(" ")
        val lineas = mutableListOf<String>()
        var lineaActual = StringBuilder()

        for (p in palabras) {
            if (lineaActual.length + p.length + 1 <= maxChars) {
                if (lineaActual.isNotEmpty()) lineaActual.append(" ")
                lineaActual.append(p)
            } else {
                if (lineaActual.isNotEmpty()) lineas.add(lineaActual.toString())
                lineaActual = StringBuilder(p)
            }
        }
        if (lineaActual.isNotEmpty()) lineas.add(lineaActual.toString())
        return lineas
    }
}

@Composable
fun DetallePeriodoScreen(
    tituloPeriodo: String,
    rangoFechas: String,
    resumen: ResumenPeriodoData?,
    diagnostico: DiagnosticoFinanciero?,
    onBackClick: () -> Unit,
    onCategoriaClick: (CategoriaGastoItem) -> Unit,
    onDiagnosticoClick: () -> Unit,
    onExportarPdfClick: () -> Unit,
    onExportarCsvClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val balanceNeto = resumen?.balanceNeto ?: 0.0
    val totalIngresos = resumen?.totalIngresos ?: 0.0
    val totalGastos = resumen?.totalGastos ?: 0.0
    val categorias = resumen?.listaCategorias.orEmpty()

    val proyeccionTexto = remember(diagnostico) {
        if (diagnostico == null) {
            "Al ritmo de gasto actual, cerrarás este periodo con un saldo estimado positivo."
        } else {
            val iconoEstado = when (diagnostico.estadoGeneral) {
                "Excelente", "Saludable" -> "🟢"
                "En Riesgo" -> "🟡"
                else -> "🔴"
            }
            "$iconoEstado ${diagnostico.estadoGeneral} (${diagnostico.puntajeSalud}/100): ${diagnostico.resumenEjecutivo}"
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.surfaceBg,
        topBar = {
            Surface(
                color = colors.surfaceCard,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Volver",
                            tint = colors.textPrimary
                        )
                    }

                    Text(
                        text = tituloPeriodo,
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = colors.surfaceCard,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onDiagnosticoClick,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryEmerald,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_insights),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Diagnóstico", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onExportarPdfClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_form),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExportarCsvClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = colors.surfaceContainerLow,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = rangoFechas,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = "BALANCE DEL PERIODO",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = format.format(balanceNeto),
                            color = if (balanceNeto < 0) colors.expenseRed else colors.primaryEmerald,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = colors.surfaceContainerHigh
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ingresos", color = colors.textSecondary, fontSize = 12.sp)
                                Text(
                                    text = "+${format.format(totalIngresos)}",
                                    color = colors.primaryEmerald,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gastos", color = colors.textSecondary, fontSize = 12.sp)
                                Text(
                                    text = "-${format.format(totalGastos)}",
                                    color = colors.expenseRed,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.primaryEmerald50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_upward),
                                contentDescription = "Proyección",
                                tint = colors.primaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = "Proyección al Cierre de Periodo",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = proyeccionTexto,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gastos por Categoría",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${categorias.size} categorías",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            items(categorias) { item ->
                CategoriaResumenItem(
                    item = item,
                    onClick = { onCategoriaClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCategoriaBottomSheet(
    item: CategoriaGastoItem,
    movimientos: List<Movimiento>,
    onDismiss: () -> Unit
) {
    val colors = DabaTheme.colors
    val format = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val grupos = remember(movimientos) { NotasAdaptador.procesarGrupos(movimientos) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.nombreCategoria,
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.porcentaje}% del total",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "-${format.format(item.totalMonto)}",
                    color = colors.expenseRed,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(grupos) { listItem ->
                    when (listItem) {
                        is MovimientoListItem.Header -> MovimientoHeaderItem(
                            fechaTitulo = listItem.fechaTitulo,
                            conteoTexto = listItem.conteoTexto
                        )
                        is MovimientoListItem.Item -> MovimientoItem(
                            movimiento = listItem.movimiento,
                            onEditarClick = { onDismiss() },
                            onEliminarClick = { onDismiss() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryEmerald)
            ) {
                Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetallePeriodoScreenPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        DetallePeriodoScreen(
            tituloPeriodo = "Resumen Mensual",
            rangoFechas = "📅 2026-11-01  a  2026-11-30",
            resumen = ResumenPeriodoData(
                totalIngresos = 3500000.0,
                totalGastos = 1200000.0,
                balanceNeto = 2300000.0,
                listaCategorias = listOf(
                    CategoriaGastoItem("Supermercado", 600000.0, 50.0, 4),
                    CategoriaGastoItem("Transporte", 300000.0, 25.0, 6)
                )
            ),
            diagnostico = null,
            onBackClick = {},
            onCategoriaClick = {},
            onDiagnosticoClick = {},
            onExportarPdfClick = {},
            onExportarCsvClick = {}
        )
    }
}
