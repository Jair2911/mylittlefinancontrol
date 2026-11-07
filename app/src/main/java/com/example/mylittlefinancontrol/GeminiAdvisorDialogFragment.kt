package com.example.mylittlefinancontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.ui.components.ChatMensajeItem
import com.example.mylittlefinancontrol.ui.theme.DabaTheme
import com.example.mylittlefinancontrol.ui.theme.MyLittleFinanControlTheme
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class MensajeChat(val texto: String, val esUsuario: Boolean)

class GeminiAdvisorDialogFragment : BottomSheetDialogFragment() {

    private val mensajes = mutableStateListOf<MensajeChat>()
    private var isLoading by mutableStateOf(false)
    private lateinit var dbHelper: NotasDatabaseHelper

    // Sesión de chat continua y modelos con fallback en cascada
    private var activeChat: Chat? = null
    private var activeModelName: String? = null
    private val modelosFallback = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash-exp", "gemini-1.5-flash-8b")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dbHelper = NotasDatabaseHelper.getInstance(requireContext())
        if (mensajes.isEmpty()) {
            mensajes.add(
                MensajeChat(
                    "¡Hola! Soy DABAFINN, tu asesor financiero personal. Tengo acceso a tu balance, deudas, ahorros y gastos recientes. ¿Qué deseas consultar hoy?",
                    false
                )
            )
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyLittleFinanControlTheme {
                    GeminiAdvisorSheetContent(
                        mensajes = mensajes,
                        isLoading = isLoading,
                        onEnviarMensaje = { textoUsuario ->
                            mensajes.add(MensajeChat(textoUsuario, true))
                            isLoading = true
                            enviarMensajeConAdaptabilidad(textoUsuario)
                        }
                    )
                }
            }
        }
    }

    /**
     * Preprocesa el mensaje con un detector ligero de complejidad y lo envía a la sesión continua de Chat.
     */
    private fun enviarMensajeConAdaptabilidad(textoUsuario: String) {
        val directrizComplejidad = evaluarComplejidadPregunta(textoUsuario)
        val promptInterno = "$textoUsuario\n$directrizComplejidad"

        lifecycleScope.launch {
            try {
                val systemInstructionText = construirSystemInstruction()
                var respuestaTexto: String? = null
                var ultimoError: Exception? = null

                // 1. Intentar enviar mensaje en la sesión de Chat activa existente
                if (activeChat != null) {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            activeChat!!.sendMessage(promptInterno)
                        }
                        if (!response.text.isNullOrBlank()) {
                            respuestaTexto = response.text
                        }
                    } catch (e: Exception) {
                        ultimoError = e
                        activeChat = null // Forzar reinicialización con fallback si falló
                    }
                }

                // 2. Si no hay chat activo o falló el modelo actual, ejecutar cascada de fallback
                if (respuestaTexto.isNullOrBlank()) {
                    val historialPrevio: List<Content> = activeChat?.history ?: emptyList()

                    for (nombreModelo in modelosFallback) {
                        if (nombreModelo == activeModelName && ultimoError != null) continue

                        try {
                            val generativeModel = GenerativeModel(
                                modelName = nombreModelo,
                                apiKey = BuildConfig.GEMINI_API_KEY,
                                systemInstruction = content { text(systemInstructionText) }
                            )

                            val nuevoChat = generativeModel.startChat(history = historialPrevio)
                            val response = withContext(Dispatchers.IO) {
                                nuevoChat.sendMessage(promptInterno)
                            }

                            if (!response.text.isNullOrBlank()) {
                                activeChat = nuevoChat
                                activeModelName = nombreModelo
                                respuestaTexto = response.text
                                break
                            }
                        } catch (e: Exception) {
                            ultimoError = e
                        }
                    }
                }

                isLoading = false
                if (!respuestaTexto.isNullOrBlank()) {
                    mensajes.add(MensajeChat(respuestaTexto, false))
                } else {
                    val errStr = ultimoError?.localizedMessage ?: ""
                    val msg = when {
                        errStr.contains("429") || errStr.contains("Quota", true) ->
                            "Límite de consultas alcanzado. Por favor espera 10 segundos antes de enviar otra pregunta."
                        else ->
                            "El servidor de Google IA se encuentra ocupado. Por favor reintenta tu pregunta en unos segundos."
                    }
                    mensajes.add(MensajeChat(msg, false))
                }
            } catch (e: Exception) {
                isLoading = false
                mensajes.add(MensajeChat("Error de conexión al procesar la respuesta. Por favor intenta nuevamente.", false))
            }
        }
    }

    /**
     * Detector ligero de complejidad para guiar la respuesta del modelo hacia la brevedad o la profundidad.
     */
    private fun evaluarComplejidadPregunta(pregunta: String): String {
        val pLower = pregunta.lowercase(Locale.ROOT).trim()

        val palabrasClaveEstrategicas = listOf(
            "plan", "analiza", "analizar", "analisis", "análisis", "detalle", "detallado",
            "estrategia", "profundo", "completo", "proyección", "proyeccion", "cómo salgo",
            "como salgo", "recomendación completa", "recomendacion completa", "diagnóstico",
            "diagnostico", "presupuesto", "organizar", "optimizar", "reestructurar", "evalúa", "evalua"
        )

        val esCompleja = palabrasClaveEstrategicas.any { pLower.contains(it) } || pLower.length > 130

        return if (esCompleja) {
            "[INSTRUCCIÓN INTERNA: El usuario solicita un análisis profundo. Realiza un diagnóstico detallado paso a paso cruzando sus ingresos, gastos fijos, deudas y movimientos.]"
        } else {
            "[INSTRUCCIÓN INTERNA: El usuario hizo una consulta simple. Responde de forma ultra concisa en máximo 3 líneas con el dato exacto del snapshot.]"
        }
    }

    /**
     * Construye las directrices maestras de DABAFINN e inyecta el snapshot financiero dentro de systemInstruction.
     */
    private fun construirSystemInstruction(): String {
        return """
            Eres DABAFINN, el asesor financiero personal inteligente integrado en la app. Tienes acceso al contexto financiero real del usuario en pesos colombianos (COP).

            REGLA DE ORO DE PROPORCIONALIDAD (Adapta tu respuesta a la pregunta):
            1. PREGUNTAS SENCILLAS O PUNTUALES (ej. saludos, "¿cuánto gasté en comida?", "¿me alcanza para gastar ${'$'}X?", "¿cuál es mi deuda más alta?"):
               - Responde en MÁXIMO 2 a 4 líneas (1 párrafo corto).
               - Da el dato numérico exacto primero y añade solo 1 frase breve de consejo directo. Prohibido dar listas largas o resúmenes no solicitados.
            2. PREGUNTAS INTERMEDIAS (ej. "¿cómo voy este mes?", "¿qué opinas de mis gastos?"):
               - Responde de forma ejecutiva con un resumen de 2 líneas y máximo 3 puntos clave (bullets) con montos exactos.
            3. PREGUNTAS COMPLEJAS O ESTRATÉGICAS (ej. "hazme un plan para pagar mis deudas", "analiza a fondo mis finanzas", "¿cómo logro mi meta de ahorro de aquí a fin de año?"):
               - Despliega un análisis financiero completo, estructurado, calculando porcentajes, capacidad de ahorro, priorización de deudas (método avalancha o bola de nieve según tasas de interés) y un plan de acción paso a paso.

            Tono: Cercano, profesional, claro y directo. Usa formato limpio sin rellenos innecesarios.

            ${generarSnapshotFinanciero()}
        """.trimIndent()
    }

    /**
     * Genera el snapshot completo de finanzas en pesos colombianos (COP).
     */
    private fun generarSnapshotFinanciero(): String {
        val resumen = dbHelper.getResumenFinanciero()
        val gastosFijos = dbHelper.getAllGastosFijos()
        val deudas = dbHelper.getAllDeudas()
        val ahorros = dbHelper.getAllAhorros()
        val ultimosMovimientos = dbHelper.getAllNotas().take(20)

        val cal = Calendar.getInstance()
        val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

        val sb = StringBuilder()
        sb.append("=== CONTEXTO FINANCIERO ACTUAL DEL USUARIO (SNAPSHOT EN COP) ===\n")
        sb.append("• Balance General (Ingresos - Gastos): ${format.format(resumen.balanceTotal)}\n")
        sb.append("• Total Gastos Fijos Mensuales: ${format.format(resumen.totalGastosFijos)}\n")
        sb.append("• Total Deudas Pendientes: ${format.format(resumen.totalDeudasPendientes)}\n\n")

        sb.append("GASTOS FIJOS DEL USUARIO:\n")
        if (gastosFijos.isEmpty()) {
            sb.append("  (No hay gastos fijos registrados)\n")
        } else {
            gastosFijos.forEach { gf ->
                val estado = if (gf.periodo == "Quincenal") {
                    val q1 = if (gf.ultimoPagoQ1 == mesAnioActual) "Q1 Pagada" else "Q1 Pendiente"
                    val q2 = if (gf.ultimoPagoQ2 == mesAnioActual) "Q2 Pagada" else "Q2 Pendiente"
                    "$q1, $q2"
                } else {
                    if (gf.ultimoPagoQ1 == mesAnioActual) "Pagado" else "Pendiente"
                }
                sb.append("  * ${gf.concepto}: ${format.format(gf.monto)} (${gf.periodo}) | Estado este mes: $estado\n")
            }
        }

        sb.append("\nDEUDAS ACTIVAS DEL USUARIO:\n")
        if (deudas.isEmpty()) {
            sb.append("  (No hay deudas activas registradas)\n")
        } else {
            deudas.forEach { d ->
                val restante = (d.totalDeuda - d.totalPagado).coerceAtLeast(0.0)
                val cuotaInfo = if (d.valorCuota > 0) " | Cuota: ${format.format(d.valorCuota)}" else ""
                val tasaInfo = if (d.tasaInteres > 0) " | Tasa: ${d.tasaInteres}% (${d.tipoInteres})" else ""
                sb.append("  * ${d.concepto} (${d.tipo}): Saldo pendiente ${format.format(restante)} de ${format.format(d.totalDeuda)}$cuotaInfo$tasaInfo\n")
            }
        }

        sb.append("\nMETAS DE AHORRO DEL USUARIO:\n")
        if (ahorros.isEmpty()) {
            sb.append("  (No hay planes de ahorro registrados)\n")
        } else {
            ahorros.forEach { a ->
                val pct = if (a.montoObjetivo > 0) ((a.montoActual / a.montoObjetivo) * 100).toInt() else 0
                sb.append("  * ${a.titulo} (${a.categoria}): ${format.format(a.montoActual)} de ${format.format(a.montoObjetivo)} ($pct%) | Fecha meta: ${a.fechaMeta}\n")
            }
        }

        sb.append("\nÚLTIMOS 20 MOVIMIENTOS REGISTRADOS:\n")
        if (ultimosMovimientos.isEmpty()) {
            sb.append("  (Sin movimientos recientes)\n")
        } else {
            ultimosMovimientos.forEach { m ->
                val fecha = if (m.fecha.isNotBlank()) m.fecha else m.fechaCreacion
                sb.append("  * $fecha | ${m.tipo_reg}: ${m.descripcion} (${m.categoria}) -> ${format.format(m.valor)}\n")
            }
        }

        return sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeChat = null
    }
}

@Composable
fun GeminiAdvisorSheetContent(
    mensajes: List<MensajeChat>,
    isLoading: Boolean,
    onEnviarMensaje: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DabaTheme.colors
    var textoInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.borderSubtle)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Asistente DABAFINN IA",
                        color = colors.primaryEmerald,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Análisis y recomendaciones inteligentes",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_lightbulb),
                    contentDescription = null,
                    tint = colors.primaryEmerald,
                    modifier = Modifier.size(32.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(bottom = 12.dp)
            ) {
                items(mensajes) { msg ->
                    ChatMensajeItem(mensaje = msg)
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                        .size(28.dp),
                    color = colors.primaryEmerald,
                    strokeWidth = 2.5.dp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textoInput,
                    onValueChange = { textoInput = it },
                    placeholder = { Text("Haz una consulta a DABAFINN...") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                IconButton(
                    onClick = {
                        val trimmed = textoInput.trim()
                        if (trimmed.isNotBlank()) {
                            textoInput = ""
                            onEnviarMensaje(trimmed)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.primaryEmerald,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar mensaje"
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GeminiAdvisorSheetPreview() {
    MyLittleFinanControlTheme(darkTheme = false) {
        GeminiAdvisorSheetContent(
            mensajes = listOf(
                MensajeChat("¡Hola! Soy DABAFINN, tu asesor financiero personal.", false),
                MensajeChat("¿Cómo van mis ahorros este mes?", true)
            ),
            isLoading = false,
            onEnviarMensaje = {}
        )
    }
}
