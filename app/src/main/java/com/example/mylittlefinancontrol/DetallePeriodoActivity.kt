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
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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

    private var resumenActual: ResumenPeriodoData? = null
    private var listaMovimientosPeriodo: List<Movimiento> = emptyList()
    private var reporteIAUltimo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetallePeriodoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotasDatabaseHelper(this)
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
                binding.tvTituloPeriodo.text = "Resumen Semanal"
            }
            "MENSUAL" -> {
                val calStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                startDate = sdf.format(calStart.time)
                binding.tvTituloPeriodo.text = "Resumen Mensual"
            }
            "SEMESTRAL" -> {
                val calStart = Calendar.getInstance().apply { add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }
                startDate = sdf.format(calStart.time)
                binding.tvTituloPeriodo.text = "Resumen Semestral"
            }
            else -> { // ANUAL
                val calStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }
                startDate = sdf.format(calStart.time)
                binding.tvTituloPeriodo.text = "Resumen Anual"
            }
        }

        binding.tvRangoFechasPeriodo.text = "📅 $startDate  a  $endDate"
    }

    private fun cargarDatosPeriodo() {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        resumenActual = db.getResumenPorRangoFechas(startDate, endDate)
        listaMovimientosPeriodo = db.getMovimientosPorRangoFechas(startDate, endDate)

        val res = resumenActual ?: return

        binding.tvBalanceNetoPeriodo.text = format.format(res.balanceNeto)
        binding.tvIngresosPeriodo.text = "+${format.format(res.totalIngresos)}"
        binding.tvGastosPeriodo.text = "-${format.format(res.totalGastos)}"

        val colorBalance = if (res.balanceNeto < 0) 
            ContextCompat.getColor(this, R.color.expense_red) 
        else 
            ContextCompat.getColor(this, R.color.primary_emerald)
        binding.tvBalanceNetoPeriodo.setTextColor(colorBalance)

        // Proyección Inteligente al Cierre
        if (res.totalIngresos > 0) {
            val margenAhorro = ((res.balanceNeto / res.totalIngresos) * 100).toInt()
            if (res.balanceNeto >= 0) {
                binding.tvProyeccionTexto.text = "🟢 Al ritmo de gasto actual, proyectas cerrar este periodo con un margen positivo del $margenAhorro% sobre tus ingresos."
            } else {
                binding.tvProyeccionTexto.text = "🔴 Tus gastos superan tus ingresos en este periodo. Alerta de sobregiro estimada de ${format.format(res.balanceNeto)}."
            }
        } else if (res.totalGastos > 0) {
            binding.tvProyeccionTexto.text = "⚠️ Se han registrado gastos por ${format.format(res.totalGastos)} sin ingresos reportados aún en este periodo."
        } else {
            binding.tvProyeccionTexto.text = "ℹ️ No se registran movimientos financieros en el rango de fechas seleccionado."
        }

        // Categorías
        val categorias = res.listaCategorias
        binding.tvTotalCategoriasCount.text = "${categorias.size} categorías"

        binding.rvCategoriasPeriodo.layoutManager = LinearLayoutManager(this)
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

    private fun mostrarBottomSheetDetalleCategoria(item: CategoriaGastoItem) {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val bsBinding = DialogDetalleCategoriaBsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        dialog.setContentView(bsBinding.root)

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

        // Botón 1: Analítica IA Gemini
        binding.btnAnaliticaIa.setOnClickListener {
            ejecutarAnaliticaIAGemini()
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

    private fun ejecutarAnaliticaIAGemini() {
        val res = resumenActual ?: return
        if (res.totalIngresos == 0.0 && res.totalGastos == 0.0) {
            Toast.makeText(this, "No hay suficiente información para analizar en este periodo", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonPayload = JSONObject().apply {
            put("periodo", tipoPeriodo)
            put("fecha_inicio", startDate)
            put("fecha_fin", endDate)
            put("total_ingresos", res.totalIngresos)
            put("total_gastos", res.totalGastos)
            put("balance_neto", res.balanceNeto)

            val catsArray = JSONArray()
            for (c in res.listaCategorias) {
                val catObj = JSONObject().apply {
                    put("categoria", c.nombreCategoria)
                    put("monto", c.totalMonto)
                    put("porcentaje", c.porcentaje)
                }
                catsArray.put(catObj)
            }
            put("categorias_principales", catsArray)
        }

        val promptSistema = """
            Rol: Eres un asesor financiero personal experto, integrado en la app DABAFINN. Tu objetivo es analizar el resumen financiero del usuario y ofrecer consejos rápidos, claros y accionables.

            Instrucciones:
            1. Analiza el JSON proporcionado que contiene los ingresos, gastos y categorías principales del periodo.
            2. Redacta un análisis de máximo 3 párrafos cortos.
            3. Destaca lo positivo (ej. buena tasa de ahorro) y alerta sobre posibles riesgos (ej. un gasto muy alto en una sola categoría).
            4. Termina con 2 recomendaciones concretas para optimizar su presupuesto en el próximo periodo.
            5. Usa un tono motivador, directo y amigable. No uses lenguaje técnico complejo.

            JSON del periodo:
            $jsonPayload
        """.trimIndent()

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Analizando con Gemini IA...")
            .setMessage("Procesando patrones y tendencias financieras del periodo $tipoPeriodo...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            val safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
            )

            val modelos = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash-exp", "gemini-1.5-flash-8b")
            var respuestaTexto: String? = null
            var ultimoError: Exception? = null

            for (nombreModelo in modelos) {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = nombreModelo,
                        apiKey = BuildConfig.GEMINI_API_KEY,
                        safetySettings = safetySettings
                    )
                    val response = generativeModel.generateContent(content { text(promptSistema) })
                    if (!response.text.isNullOrBlank()) {
                        respuestaTexto = response.text
                        break
                    }
                } catch (e: Exception) {
                    ultimoError = e
                }
            }

            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                if (!respuestaTexto.isNullOrBlank()) {
                    reporteIAUltimo = respuestaTexto!!
                    AlertDialog.Builder(this@DetallePeriodoActivity)
                        .setTitle("🤖 Diagnóstico IA - DABAFINN")
                        .setMessage(respuestaTexto)
                        .setPositiveButton("Entendido", null)
                        .setNeutralButton("Exportar PDF con IA") { _, _ -> exportarReportePDF() }
                        .show()
                } else {
                    val errStr = ultimoError?.localizedMessage ?: ""
                    val msg = when {
                        errStr.contains("429") || errStr.contains("Quota", true) -> "Límite de consultas por minuto alcanzado. Espera unos segundos e intenta de nuevo."
                        else -> "El servidor de Google IA se encuentra en alta demanda. Intenta de nuevo en unos segundos."
                    }
                    Toast.makeText(this@DetallePeriodoActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
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

        if (reporteIAUltimo.isNotBlank()) {
            y += 25f
            canvas.drawText("ANÁLISIS E INSIGHTS DE INTELIGENCIA ARTIFICIAL", 40f, y, paintTitle)
            y += 20f
            val lineasIA = reporteIAUltimo.split("\n")
            for (l in lineasIA.take(15)) {
                if (y > 800f) break
                val lClean = if (l.length > 70) l.substring(0, 70) + "..." else l
                canvas.drawText(lClean, 40f, y, paintSub)
                y += 16f
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
}
