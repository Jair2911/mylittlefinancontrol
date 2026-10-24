package com.example.mylittlefinancontrol

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylittlefinancontrol.Modelos.CategoriaGastoItem
import com.example.mylittlefinancontrol.Modelos.ResumenPeriodoData
import com.example.mylittlefinancontrol.databinding.ActivityDetallePeriodoBinding
import com.example.mylittlefinancontrol.databinding.DialogDetalleCategoriaBsBinding
import com.example.mylittlefinancontrol.databinding.ItemCategoriaResumenBinding
import com.example.mylittlefinancontrol.Servicios.AnalizadorFinancieroLocal
import com.example.mylittlefinancontrol.Servicios.DiagnosticoFinanciero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetallePeriodoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetallePeriodoBinding
    private lateinit var db: NotasDatabaseHelper

    private var tipoPeriodo: String = "MENSUAL"
    private var startDate: String = ""
    private var endDate: String = ""
    private var prevStartDate: String = ""
    private var prevEndDate: String = ""

    private var resumenActual: ResumenPeriodoData? = null
    private var listaMovimientosPeriodo: List<Movimiento> = emptyList()
    private var diagnosticoActual: DiagnosticoFinanciero? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetallePeriodoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotasDatabaseHelper.getInstance(this)
        tipoPeriodo = intent.getStringExtra("TIPO_PERIODO") ?: "MENSUAL"

        calcularRangosFechas()
        cargarDatosPeriodo()
        setupListeners()
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

                binding.tvTituloPeriodo.text = "Resumen Semanal"
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

                binding.tvTituloPeriodo.text = "Resumen Mensual"
            }
            "SEMESTRAL" -> {
                val calStart = Calendar.getInstance().apply { add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }
                startDate = sdf.format(calStart.time)

                val calPrevStart = Calendar.getInstance().apply { add(Calendar.MONTH, -11); set(Calendar.DAY_OF_MONTH, 1) }
                val calPrevEnd = Calendar.getInstance().apply { add(Calendar.MONTH, -6); set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
                prevStartDate = sdf.format(calPrevStart.time)
                prevEndDate = sdf.format(calPrevEnd.time)

                binding.tvTituloPeriodo.text = "Resumen Semestral"
            }
            else -> { // ANUAL
                val calStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }
                startDate = sdf.format(calStart.time)

                val calPrevStart = Calendar.getInstance().apply { add(Calendar.YEAR, -1); set(Calendar.DAY_OF_YEAR, 1) }
                val calPrevEnd = Calendar.getInstance().apply { add(Calendar.YEAR, -1); set(Calendar.MONTH, Calendar.DECEMBER); set(Calendar.DAY_OF_MONTH, 31) }
                prevStartDate = sdf.format(calPrevStart.time)
                prevEndDate = sdf.format(calPrevEnd.time)

                binding.tvTituloPeriodo.text = "Resumen Anual"
            }
        }

        binding.tvRangoFechasPeriodo.text = "📅 $startDate  a  $endDate"
    }

    private fun cargarDatosPeriodo() {
        lifecycleScope.launch(Dispatchers.IO) {
            val res = db.getResumenPorRangoFechas(startDate, endDate)
            val movs = db.getMovimientosPorRangoFechas(startDate, endDate)
            val gastosAnteriores = db.getTotalGastosPeriodoSQL(prevStartDate, prevEndDate)
            val totalGF = db.getAllGastosFijos().sumOf { it.monto }
            val totalDeudas = db.getAllDeudas().filter { (it.totalDeuda - it.totalPagado) > 0 }.sumOf { it.valorCuota }

            // Generar Diagnóstico Financiero Local (Determinista, 100% Offline)
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

            resumenActual = res
            listaMovimientosPeriodo = movs
            diagnosticoActual = diagnostico

            withContext(Dispatchers.Main) {
                val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

                binding.tvBalanceNetoPeriodo.text = format.format(res.balanceNeto)
                binding.tvIngresosPeriodo.text = "+${format.format(res.totalIngresos)}"
                binding.tvGastosPeriodo.text = "-${format.format(res.totalGastos)}"

                val colorBalance = if (res.balanceNeto < 0) 
                    ContextCompat.getColor(this@DetallePeriodoActivity, R.color.expense_red) 
                else 
                    ContextCompat.getColor(this@DetallePeriodoActivity, R.color.primary_emerald)
                binding.tvBalanceNetoPeriodo.setTextColor(colorBalance)

                // Diagnóstico Financiero Inmediato en Pantalla
                val iconoEstado = when (diagnostico.estadoGeneral) {
                    "Excelente", "Saludable" -> "🟢"
                    "En Riesgo" -> "🟡"
                    else -> "🔴"
                }
                binding.tvProyeccionTexto.text = "$iconoEstado ${diagnostico.estadoGeneral} (${diagnostico.puntajeSalud}/100): ${diagnostico.resumenEjecutivo}"

                // Categorías
                val categorias = res.listaCategorias
                binding.tvTotalCategoriasCount.text = "${categorias.size} categorías"

                binding.rvCategoriasPeriodo.layoutManager = LinearLayoutManager(this@DetallePeriodoActivity)
                val adapterCats = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        val b = ItemCategoriaResumenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                        return object : RecyclerView.ViewHolder(b.root) {}
                    }

                    override fun getItemCount() = categorias.size

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                        val item = categorias[position]
                        val b = ItemCategoriaResumenBinding.bind(holder.itemView)

                        b.tvNombreCategoria.text = item.nombreCategoria
                        b.tvMontoCategoria.text = "-${format.format(item.totalMonto)}"
                        b.tvPorcentajeCategoria.text = "${item.porcentaje}%"
                        b.tvConteoTransacciones.text = "${item.conteoTransacciones} transacciones"
                        b.pbPorcentajeCategoria.progress = item.porcentaje.toInt().coerceIn(0, 100)

                        holder.itemView.setOnClickListener {
                            mostrarBottomSheetDetalleCategoria(item)
                        }
                    }
                }
                binding.rvCategoriasPeriodo.adapter = adapterCats
            }
        }
    }

    private fun mostrarBottomSheetDetalleCategoria(item: CategoriaGastoItem) {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val bsBinding = DialogDetalleCategoriaBsBinding.inflate(layoutInflater)
        val dialog = android.app.Dialog(this)
        dialog.setContentView(bsBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        bsBinding.tvTituloCategoriaBs.text = item.nombreCategoria
        bsBinding.tvMontoTotalBs.text = "-${format.format(item.totalMonto)}"
        bsBinding.tvPorcentajeBs.text = "${item.porcentaje}% del total"

        val movsCategoria = listaMovimientosPeriodo.filter { mov ->
            val cat = if (mov.categoria.isNotBlank()) mov.categoria else "Varios"
            cat.equals(item.nombreCategoria, ignoreCase = true) && !mov.tipo_reg.contains("INGRESO", true)
        }

        bsBinding.rvMovimientosCategoriaBs.layoutManager = LinearLayoutManager(this)
        val adaptadorMovs = NotasAdaptador(
            listaOriginal = movsCategoria,
            context = this,
            onEditarClick = { dialog.dismiss() },
            onEliminarClick = { dialog.dismiss() }
        )
        bsBinding.rvMovimientosCategoriaBs.adapter = adaptadorMovs

        bsBinding.btnCerrarBs.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupListeners() {
        binding.btnVolverPeriodo.setOnClickListener { finish() }

        // Botón 1: Diagnóstico Financiero Local (Offline e Instantáneo)
        binding.btnAnaliticaIa.text = "Diagnóstico"
        binding.btnAnaliticaIa.setOnClickListener {
            mostrarDiagnosticoLocal()
        }

        // Botón 2: Exportar PDF
        binding.btnExportarPdf.setOnClickListener {
            exportarReportePDF()
        }

        // Botón 3: Exportar CSV
        binding.btnExportarCsv.setOnClickListener {
            exportarReporteCSV()
        }
    }

    private fun mostrarDiagnosticoLocal() {
        val diag = diagnosticoActual ?: return
        if (resumenActual?.totalIngresos == 0.0 && resumenActual?.totalGastos == 0.0) {
            Toast.makeText(this, "No hay suficiente información para analizar en este periodo", Toast.LENGTH_SHORT).show()
            return
        }

        val iconoEstado = when (diag.estadoGeneral) {
            "Excelente", "Saludable" -> "🟢"
            "En Riesgo" -> "🟡"
            else -> "🔴"
        }

        val mensajeDetallado = buildString {
            append("$iconoEstado Estado: ${diag.estadoGeneral.uppercase()} (Salud: ${diag.puntajeSalud}/100)\n\n")
            append("📌 Resumen Ejecutivo:\n${diag.resumenEjecutivo}\n\n")
            append("🔍 Puntos Clave:\n")
            diag.puntosClave.forEach { p ->
                append("• $p\n")
            }
            append("\n💡 Recomendación Accionable:\n${diag.recomendacionAccionable}")
        }

        AlertDialog.Builder(this)
            .setTitle("📊 Diagnóstico Financiero ($tipoPeriodo)")
            .setMessage(mensajeDetallado)
            .setPositiveButton("Entendido", null)
            .setNeutralButton("Exportar PDF") { _, _ -> exportarReportePDF() }
            .show()
    }

    private fun exportarReportePDF() {
        val res = resumenActual ?: return
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.parseColor("#0F382C")
            textSize = 20f
            isFakeBoldText = true
        }

        val paintSub = Paint().apply {
            color = Color.parseColor("#4F6354")
            textSize = 12f
        }

        val paintText = Paint().apply {
            color = Color.BLACK
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
        val res = resumenActual ?: return
        val movs = listaMovimientosPeriodo

        val csvBuilder = StringBuilder()
        csvBuilder.append("Fecha,Concepto,Tipo,Categoria,Valor\n")

        for (m in movs) {
            val fecha = if (m.fecha.isNotBlank()) m.fecha else m.fechaCreacion
            val desc = m.descripcion.replace(",", " ")
            val tipo = m.tipo_reg
            val cat = if (m.categoria.isNotBlank()) m.categoria else "Varios"
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
