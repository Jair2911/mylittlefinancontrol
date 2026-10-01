package com.example.mylittlefinancontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.databinding.LayoutChatGeminiBinding
import com.example.mylittlefinancontrol.BuildConfig
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class GeminiAdvisorDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutChatGeminiBinding? = null
    private val binding get() = _binding!!
    private val mensajes = mutableListOf<MensajeChat>()
    private lateinit var adapter: ChatAdapter
    private lateinit var dbHelper: NotasDatabaseHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutChatGeminiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        dbHelper = NotasDatabaseHelper(requireContext())
        adapter = ChatAdapter(mensajes)
        binding.rvChat.adapter = adapter

        val systemInstructionText = """
            Eres DABAFINN, un asesor financiero experto, analítico y directo. Tu objetivo es ayudar al usuario a tener libertad financiera.
            
            REGLAS DE ORO:
            1. Analiza el contexto financiero detallado que se te proporciona abajo.
            2. Si el usuario quiere gastar en algo innecesario y su balance es bajo o tiene deudas altas, sé estricto y desaconséjalo con argumentos.
            3. Si el usuario va bien, felicítalo pero sugiere ahorro o inversión.
            4. Detecta patrones en sus movimientos (ej. muchos gastos hormiga).
            5. Tus respuestas deben ser en español, claras, concisas y con un tono profesional pero cercano.
            
            ${generarContextoFinanciero()}
        """.trimIndent()

        val modelos = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash-exp", "gemini-1.5-flash-8b")
                
        agregarMensaje("¡Hola! Soy tu asesor DABAFINN. He analizado tus datos y estoy listo para darte consejos. ¿En qué puedo ayudarte?", false)
        binding.btnEnviar.setOnClickListener {
            val textoUsuario = binding.etMensaje.text.toString()
            if (textoUsuario.isNotBlank()) {
                agregarMensaje(textoUsuario, true)
                binding.etMensaje.text?.clear()
                binding.pbLoading.visibility = View.VISIBLE

                lifecycleScope.launch {
                    try {
                        var respuestaTexto: String? = null
                        var ultimoError: Exception? = null

                        val promptCompleto = "$systemInstructionText\n\nPregunta del usuario: $textoUsuario"

                        for (nombreModelo in modelos) {
                            try {
                                val generativeModel = GenerativeModel(
                                    modelName = nombreModelo,
                                    apiKey = BuildConfig.GEMINI_API_KEY
                                )
                                val response = withContext(Dispatchers.IO) {
                                    generativeModel.generateContent(promptCompleto)
                                }
                                if (!response.text.isNullOrBlank()) {
                                    respuestaTexto = response.text
                                    break
                                }
                            } catch (e: Exception) {
                                ultimoError = e
                            }
                        }

                        binding.pbLoading.visibility = View.GONE
                        if (!respuestaTexto.isNullOrBlank()) {
                            agregarMensaje(respuestaTexto, false)
                        } else {
                            agregarMensaje("Error: ${ultimoError?.localizedMessage ?: "No se pudo obtener respuesta"}", false)
                        }
                    } catch (e: Exception) {
                        binding.pbLoading.visibility = View.GONE
                        agregarMensaje("Error: ${e.message}", false)
                    }
                }
            }
        }
    }

    private fun generarContextoFinanciero(): String {
        val resumen = dbHelper.getResumenFinanciero()
        val gastosFijos = dbHelper.getAllGastosFijos()
        val deudas = dbHelper.getAllDeudas()
        val ultimosMovimientos = dbHelper.getAllNotas().take(20)
        
        val cal = Calendar.getInstance()
        val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        
        val sb = StringBuilder()
        sb.append("CONTEXTO FINANCIERO ACTUAL DEL USUARIO:\n")
        sb.append("- Balance General (Ingresos - Gastos): ${resumen.balanceTotal}\n")
        sb.append("- Total Gastos Fijos del mes: ${resumen.totalGastosFijos}\n")
        sb.append("- Total Deudas por pagar: ${resumen.totalDeudasPendientes}\n\n")
        
        sb.append("DETALLE GASTOS FIJOS:\n")
        gastosFijos.forEach { gf ->
            val estado = if (gf.periodo == "Quincenal") {
                val q1 = if (gf.ultimoPagoQ1 == mesAnioActual) "Q1 Pagada" else "Q1 Pendiente"
                val q2 = if (gf.ultimoPagoQ2 == mesAnioActual) "Q2 Pagada" else "Q2 Pendiente"
                "$q1, $q2"
            } else {
                if (gf.ultimoPagoQ1 == mesAnioActual) "Pagado" else "Pendiente"
            }
            sb.append("  * ${gf.concepto}: ${gf.monto} (${gf.periodo}) | Estado este mes: $estado\n")
        }
        
        sb.append("\nDETALLE DEUDAS ACTIVAS:\n")
        deudas.forEach { d ->
            val restante = d.totalDeuda - d.totalPagado
            sb.append("  * ${d.concepto} (${d.tipo}): Debe ${restante} de un total de ${d.totalDeuda}. Plazo: ${d.fechaInicio} a ${d.fechaFin}\n")
        }
        
        sb.append("\nÚLTIMOS 20 MOVIMIENTOS REGISTRADOS:\n")
        ultimosMovimientos.forEach { m ->
            sb.append("  * ${m.fecha} | ${m.tipo_reg}: ${m.descripcion} por ${m.valor}\n")
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
        _binding = null
    }
}
