package com.example.mylittlefinancontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.databinding.LayoutChatGeminiBinding
import com.example.mylittlefinancontrol.BuildConfig
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

class GeminiAdvisorDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutChatGeminiBinding? = null
    private val binding get() = _binding!!
    private val mensajes = mutableListOf<MensajeChat>()
    private lateinit var adapter: ChatAdapter
    private lateinit var dbHelper: NotasDatabaseHelper

    // Sesión de chat continua y modelos con fallback en cascada
    private var activeChat: Chat? = null
    private var activeModelName: String? = null
    private val modelosFallback = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash-exp", "gemini-1.5-flash-8b")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutChatGeminiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = NotasDatabaseHelper.getInstance(requireContext())
        adapter = ChatAdapter(mensajes)
        binding.rvChat.adapter = adapter

        agregarMensaje("¡Hola! Soy DABAFINN, tu asesor financiero personal. Tengo acceso a tu balance, deudas, ahorros y gastos recientes. ¿Qué deseas consultar hoy?", false)

        binding.btnEnviar.setOnClickListener {
            val textoUsuario = binding.etMensaje.text.toString().trim()
            if (textoUsuario.isNotBlank()) {
                // Mostrar solo el texto original del usuario en la interfaz del chat
                agregarMensaje(textoUsuario, true)
                binding.etMensaje.text?.clear()
                binding.pbLoading.visibility = View.VISIBLE

                enviarMensajeConAdaptabilidad(textoUsuario)
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

                            // Continuidad de conversación pasando el historial previo
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

                binding.pbLoading.visibility = View.GONE
                if (!respuestaTexto.isNullOrBlank()) {
                    agregarMensaje(respuestaTexto, false)
                } else {
                    val errStr = ultimoError?.localizedMessage ?: ""
                    val msg = when {
                        errStr.contains("429") || errStr.contains("Quota", true) ->
                            "Límite de consultas alcanzado. Por favor espera 10 segundos antes de enviar otra pregunta."
                        else ->
                            "El servidor de Google IA se encuentra ocupado. Por favor reintenta tu pregunta en unos segundos."
                    }
                    agregarMensaje(msg, false)
                }
            } catch (e: Exception) {
                binding.pbLoading.visibility = View.GONE
                agregarMensaje("Error de conexión al procesar la respuesta. Por favor intenta nuevamente.", false)
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

    private fun agregarMensaje(texto: String, esUsuario: Boolean) {
        mensajes.add(MensajeChat(texto, esUsuario))
        adapter.notifyItemInserted(mensajes.size - 1)
        binding.rvChat.scrollToPosition(mensajes.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeChat = null
        _binding = null
    }
}
