package com.example.mylittlefinancontrol

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.databinding.LayoutChatGeminiBinding
import com.example.mylittlefinancontrol.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        agregarMensaje("¡Hola! Soy tu asesor DABAFINN. ¿En qué puedo ayudarte hoy?", false)

        val resumen = dbHelper.getResumenFinanciero()
        val systemInstructionText = """
            Eres un asesor financiero estricto y analítico. Evalúa las ideas financieras del usuario con base en su realidad. 
            Datos actuales del usuario: [Balance: ${resumen.balanceTotal}], [Gastos Fijos: ${resumen.totalGastosFijos}], [Deudas: ${resumen.totalDeudasPendientes}]. 
            Si la consulta del usuario lo dejaría sin liquidez, Si es viable, dile bajo qué condiciones. Responde de forma directa, corta y coloquial.
        """.trimIndent()

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text(systemInstructionText) }
        )

        binding.btnEnviar.setOnClickListener {
            val textoUsuario = binding.etMensaje.text.toString()
            if (textoUsuario.isNotBlank()) {
                agregarMensaje(textoUsuario, true)
                binding.etMensaje.text.clear()
                binding.pbLoading.visibility = View.VISIBLE

                lifecycleScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            generativeModel.generateContent(textoUsuario)
                        }
                        binding.pbLoading.visibility = View.GONE
                        agregarMensaje(response.text ?: "No se pudo obtener respuesta", false)
                    } catch (e: Exception) {
                        binding.pbLoading.visibility = View.GONE
                        agregarMensaje("Error: ${e.message}", false)
                    }
                }
            }
        }
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
