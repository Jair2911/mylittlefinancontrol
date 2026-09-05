package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylittlefinancontrol.Adaptadores.MovimientoInicioAdapter
import com.example.mylittlefinancontrol.GeminiAdvisorDialogFragment
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.FragmentInicioBinding
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

class FragmentInicio : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var movAdapter: MovimientoInicioAdapter

    private var totalGastosFijos = 0.0
    private var totalDeudasRestante = 0.0
    private var ingresosMes = 0.0
    private var gastosMes = 0.0
    private var ingresosTotales = 0.0
    private var gastosTotales = 0.0
    
    private var esIncognito: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

        setupRecyclerView()
        cargarDatosLocales()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        cargarDatosLocales()
    }

    private fun setupRecyclerView() {
        movAdapter = MovimientoInicioAdapter(emptyList())
        binding.rvUltimosMovimientos.apply {
            layoutManager = LinearLayoutManager(mContext)
            adapter = movAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun cargarDatosLocales() {
        val listaGF = db.getAllGastosFijos()
        totalGastosFijos = listaGF.sumOf { it.monto }

        val listaD = db.getAllDeudas()
        totalDeudasRestante = listaD.sumOf { it.totalDeuda - it.totalPagado }

        calcularMovimientosDelMes()
    }

    private fun setupClickListeners() {
        val mainActivity = activity as? MainActivity

        binding.cardBalanceGeneral.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardIngresos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardGastos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardGastosFijos.setOnClickListener { mainActivity?.verFragmentoGastosFijos() }
        binding.cardDeudas.setOnClickListener { mainActivity?.verFragmentoDeudas() }

        binding.btnVerTodosMovimientos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        
        binding.cardSaludFinanciera.setOnClickListener {
            GeminiAdvisorDialogFragment().show(parentFragmentManager, "GeminiAdvisor")
        }

        binding.btnIncognitoToggle.setOnClickListener {
            esIncognito = !esIncognito
            actualizarInterfaz()
        }

        // Acciones rápidas (Gasto, Ingreso, Transferir, Analítica)
        binding.btnQuickGasto.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.btnQuickIngreso.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.btnQuickTransferir.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.btnQuickAnalitica.setOnClickListener {
            GeminiAdvisorDialogFragment().show(parentFragmentManager, "GeminiAdvisor")
        }

        // Plan de Ahorro
        binding.btnNuevaMetaAhorro.setOnClickListener { mainActivity?.verFragmentoAhorros() }
        binding.cardPlanAhorro.setOnClickListener { mainActivity?.verFragmentoAhorros() }
        binding.rowMetaSecundaria.setOnClickListener { mainActivity?.verFragmentoAhorros() }
    }

    private fun calcularMovimientosDelMes() {
        val calendar = Calendar.getInstance()
        val mesActual = calendar.get(Calendar.MONTH)
        val anioActual = calendar.get(Calendar.YEAR)

        val todosLosMovimientos = db.getAllNotas()
        ingresosMes = 0.0
        gastosMes = 0.0
        ingresosTotales = 0.0
        gastosTotales = 0.0

        for (mov in todosLosMovimientos) {
            if (mov.tipo_reg.contains("INGRESO", true)) {
                ingresosTotales += mov.valor
            } else {
                gastosTotales += mov.valor
            }

            try {
                val partes = mov.fecha.split("-")
                val anio = partes[0].toInt()
                val mes = partes[1].toInt() - 1
                
                if (anio == anioActual && mes == mesActual) {
                    if (mov.tipo_reg.contains("INGRESO", true)) {
                        ingresosMes += mov.valor
                    } else {
                        gastosMes += mov.valor
                    }
                }
            } catch (_: Exception) {}
        }
        
        val ultimosMovimientos = if (todosLosMovimientos.size > 25) {
            todosLosMovimientos.take(25)
        } else {
            todosLosMovimientos
        }
        movAdapter.actualizarLista(ultimosMovimientos)

        actualizarInterfaz()
    }

    private fun actualizarInterfaz() {
        if (!isAdded || _binding == null) return
        
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val balanceTotal = ingresosTotales - gastosTotales

        if (esIncognito) {
            binding.tvBalanceGeneral.text = "••••••••"
            binding.tvIngresosMes.text = "••••••"
            binding.tvGastosMes.text = "••••••"
            binding.tvTotalGastosFijos.text = "••••••"
            binding.tvTotalDeudas.text = "••••••"
            binding.tvMeta1Actual.text = "••••••"
            binding.tvMeta1Total.text = "de ••••••"
            binding.tvMeta2Subtitulo.text = "••••••"
            binding.btnIncognitoToggle.setImageResource(R.drawable.ic_visibility_off)
        } else {
            val signo = if (balanceTotal < 0) "-$ " else if (balanceTotal > 0) "+$ " else "$ "
            val valorFormateado = format.format(abs(balanceTotal)).replace("$", "").trim()
            binding.tvBalanceGeneral.text = "$signo$valorFormateado"
            
            binding.tvIngresosMes.text = format.format(ingresosMes)
            binding.tvGastosMes.text = format.format(gastosMes)
            binding.tvTotalGastosFijos.text = format.format(totalGastosFijos)
            binding.tvTotalDeudas.text = format.format(totalDeudasRestante)
            binding.btnIncognitoToggle.setImageResource(R.drawable.ic_visibility)
        }

        val colorBalance = if (balanceTotal < 0) 
            R.color.text_primary 
        else 
            R.color.primary_emerald
            
        binding.tvBalanceGeneral.setTextColor(ContextCompat.getColor(mContext, colorBalance))

        // Subtextos de las tarjetas Bento
        binding.tvSubtextIngresos.text = if (ingresosMes > 0) "1 depósito activo" else "Sin depósitos aún"
        
        val cantGastosMes = db.getAllNotas().count { !it.tipo_reg.contains("INGRESO", true) }
        binding.tvSubtextGastos.text = if (cantGastosMes == 1) "1 registro activo" else "$cantGastosMes registros activos"

        val countGF = db.getAllGastosFijos().size
        binding.tvSubtextGastosFijos.text = "$countGF recurrentes"

        val countDeudas = db.getAllDeudas().size
        binding.tvSubtextDeudas.text = if (countDeudas > 0) "$countDeudas activas" else "Bajo control"

        // Actualización dinámica de metas de ahorro en Inicio
        val listaAhorros = db.getAllAhorros()
        if (listaAhorros.isNotEmpty()) {
            binding.tvAhorroBadgeActivas.text = "${listaAhorros.size} activas"
            val meta1 = listaAhorros[0]
            binding.tvMeta1Titulo.text = meta1.titulo
            binding.tvMeta1Fecha.text = if (meta1.fechaMeta.isNotEmpty()) "Meta: ${meta1.fechaMeta}" else "Sin fecha límite"
            val pct1 = if (meta1.montoObjetivo > 0) ((meta1.montoActual / meta1.montoObjetivo) * 100).toInt().coerceIn(0, 100) else 0
            binding.tvMeta1Porcentaje.text = "$pct1%"
            binding.pbMeta1.progress = pct1

            if (!esIncognito) {
                binding.tvMeta1Actual.text = format.format(meta1.montoActual)
                binding.tvMeta1Total.text = "de ${format.format(meta1.montoObjetivo)}"
            }

            if (listaAhorros.size > 1) {
                binding.rowMetaSecundaria.visibility = View.VISIBLE
                val meta2 = listaAhorros[1]
                binding.tvMeta2Titulo.text = meta2.titulo
                val pct2 = if (meta2.montoObjetivo > 0) ((meta2.montoActual / meta2.montoObjetivo) * 100).toInt().coerceIn(0, 100) else 0
                binding.tvMeta2Porcentaje.text = "$pct2%"
                if (!esIncognito) {
                    binding.tvMeta2Subtitulo.text = "${format.format(meta2.montoActual)} de ${format.format(meta2.montoObjetivo)}"
                }
            } else {
                binding.rowMetaSecundaria.visibility = View.GONE
            }
        } else {
            binding.tvAhorroBadgeActivas.text = "0 activas"
            binding.tvMeta1Titulo.text = "Sin metas activas"
            binding.tvMeta1Fecha.text = "Toca para agregar una meta"
            binding.tvMeta1Porcentaje.text = "0%"
            binding.pbMeta1.progress = 0
            if (!esIncognito) {
                binding.tvMeta1Actual.text = "$ 0"
                binding.tvMeta1Total.text = "de $ 0"
            }
            binding.rowMetaSecundaria.visibility = View.GONE
        }

        // Cálculo dinámico de salud financiera
        val pctFijos = if (ingresosMes > 0) {
            ((totalGastosFijos / ingresosMes) * 100).toInt().coerceIn(10, 95)
        } else {
            60
        }
        binding.tvSaludSubtitulo.text = "Tus compromisos fijos representan el $pctFijos%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}