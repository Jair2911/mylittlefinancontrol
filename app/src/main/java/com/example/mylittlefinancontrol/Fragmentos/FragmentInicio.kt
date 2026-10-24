package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mylittlefinancontrol.DashboardCache
import com.example.mylittlefinancontrol.DetallePeriodoActivity
import com.example.mylittlefinancontrol.GeminiAdvisorDialogFragment
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.FragmentInicioBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class FragmentInicio : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper

    private var totalGastosFijos = 0.0
    private var totalDeudasRestante = 0.0
    private var ingresosMes = 0.0
    private var gastosMes = 0.0
    private var ingresosTotales = 0.0
    private var gastosTotales = 0.0
    private var countGastosMes = 0
    private var countGF = 0
    private var countDeudas = 0
    
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
        db = NotasDatabaseHelper.getInstance(mContext)

        // 1. PINTAR INMEDIATAMENTE SI YA EXISTE LA CACHÉ EN RAM (0ms)
        val cacheInmediata = db.getCachedDashboardImmediate()
        if (cacheInmediata != null) {
            aplicarDashboardCacheEnUI(cacheInmediata)
        }

        cargarDatosLocales()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        val cacheInmediata = db.getCachedDashboardImmediate()
        if (cacheInmediata != null) {
            aplicarDashboardCacheEnUI(cacheInmediata)
        }
        cargarDatosLocales()
    }

    private fun cargarDatosLocales() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cacheData = db.getDashboardDataCached { p -> calcularFechasPeriodo(p) }
            withContext(Dispatchers.Main) {
                if (_binding == null) return@withContext
                aplicarDashboardCacheEnUI(cacheData)
            }
        }
    }

    private fun aplicarDashboardCacheEnUI(cache: DashboardCache) {
        totalGastosFijos = cache.totalGastosFijos
        totalDeudasRestante = cache.totalDeudasRestante
        ingresosMes = cache.ingresosMes
        gastosMes = cache.gastosMes
        ingresosTotales = cache.ingresosTotales
        gastosTotales = cache.gastosTotales
        countGastosMes = cache.countGastosMes
        countGF = cache.countGF
        countDeudas = cache.countDeudas

        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        for ((p, data) in cache.periodosResumen) {
            val balanceTxt = format.format(data.first)
            val varTxt = data.second
            when (p) {
                "SEMANAL" -> {
                    binding.tvBalanceSemanal.text = balanceTxt
                    binding.tvTendenciaSemanal.text = varTxt
                }
                "MENSUAL" -> {
                    binding.tvBalanceMensual.text = balanceTxt
                    binding.tvTendenciaMensual.text = varTxt
                }
                "SEMESTRAL" -> {
                    binding.tvBalanceSemestral.text = balanceTxt
                    binding.tvTendenciaSemestral.text = varTxt
                }
                "ANUAL" -> {
                    binding.tvBalanceAnual.text = balanceTxt
                    binding.tvTendenciaAnual.text = varTxt
                }
            }
        }

        actualizarInterfaz()
    }

    private fun setupClickListeners() {
        val mainActivity = activity as? MainActivity

        binding.cardBalanceGeneral.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardIngresos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardGastos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardGastosFijos.setOnClickListener { mainActivity?.verFragmentoGastosFijos() }
        binding.cardDeudas.setOnClickListener { mainActivity?.verFragmentoDeudas() }
        binding.cardAhorrosIntocable.setOnClickListener { mainActivity?.verFragmentoAhorros() }

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

        // Click listeners para los 4 resúmenes del Grid 2x2
        binding.cardResumenSemanal.setOnClickListener { abrirDetallePeriodo("SEMANAL") }
        binding.cardResumenMensual.setOnClickListener { abrirDetallePeriodo("MENSUAL") }
        binding.cardResumenSemestral.setOnClickListener { abrirDetallePeriodo("SEMESTRAL") }
        binding.cardResumenAnual.setOnClickListener { abrirDetallePeriodo("ANUAL") }
    }

    private fun abrirDetallePeriodo(tipoPeriodo: String) {
        val intent = Intent(mContext, DetallePeriodoActivity::class.java).apply {
            putExtra("TIPO_PERIODO", tipoPeriodo)
        }
        startActivity(intent)
    }

    private fun calcularFechasPeriodo(tipo: String): Pair<Pair<String, String>, Pair<String, String>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val hoy = sdf.format(cal.time)

        return when (tipo) {
            "SEMANAL" -> {
                val endActual = hoy
                val calStartAct = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                val startActual = sdf.format(calStartAct.time)

                val calEndAnt = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                val endAnt = sdf.format(calEndAnt.time)
                val calStartAnt = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -13) }
                val startAnt = sdf.format(calStartAnt.time)

                Pair(Pair(startActual, endActual), Pair(startAnt, endAnt))
            }
            "MENSUAL" -> {
                val calActIni = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                val startActual = sdf.format(calActIni.time)
                val endActual = hoy

                val calAntFin = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_YEAR, -1) }
                val endAnt = sdf.format(calAntFin.time)
                val calAntIni = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -1) }
                val startAnt = sdf.format(calAntIni.time)

                Pair(Pair(startActual, endActual), Pair(startAnt, endAnt))
            }
            "SEMESTRAL" -> {
                val endActual = hoy
                val calStartAct = Calendar.getInstance().apply { add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }
                val startActual = sdf.format(calStartAct.time)

                val calEndAnt = Calendar.getInstance().apply { time = calStartAct.time; add(Calendar.DAY_OF_YEAR, -1) }
                val endAnt = sdf.format(calEndAnt.time)
                val calStartAnt = Calendar.getInstance().apply { time = calEndAnt.time; add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }
                val startAnt = sdf.format(calStartAnt.time)

                Pair(Pair(startActual, endActual), Pair(startAnt, endAnt))
            }
            else -> { // ANUAL
                val calActIni = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }
                val startActual = sdf.format(calActIni.time)
                val endActual = hoy

                val calAntFin = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); add(Calendar.DAY_OF_YEAR, -1) }
                val endAnt = sdf.format(calAntFin.time)
                val calAntIni = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); add(Calendar.YEAR, -1) }
                val startAnt = sdf.format(calAntIni.time)

                Pair(Pair(startActual, endActual), Pair(startAnt, endAnt))
            }
        }
    }

    private fun actualizarInterfaz() {
        if (!isAdded || _binding == null) return
        
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val balanceTotal = db.getResumenFinanciero().balanceTotal // Ya tiene restados los ahorros
        val ahorrosData = db.getResumenFinanciero()

        if (esIncognito) {
            binding.tvBalanceGeneral.text = "••••••••"
            binding.tvTotalAhorros.text = "••••••••"
            binding.tvDesgloseAhorros.text = "Cuentas: •••• • Alcancía/Efectivo: ••••"
            binding.tvIngresosMes.text = "••••••"
            binding.tvGastosMes.text = "••••••"
            binding.tvTotalGastosFijos.text = "••••••"
            binding.tvTotalDeudas.text = "••••••"
            binding.tvBalanceSemanal.text = "••••••"
            binding.tvBalanceMensual.text = "••••••"
            binding.tvBalanceSemestral.text = "••••••"
            binding.tvBalanceAnual.text = "••••••"
            binding.btnIncognitoToggle.setImageResource(R.drawable.ic_visibility_off)
        } else {
            val signo = if (balanceTotal < 0) "-$ " else if (balanceTotal > 0) "+$ " else "$ "
            val valorFormateado = format.format(Math.abs(balanceTotal)).replace("$", "").trim()
            binding.tvBalanceGeneral.text = "$signo$valorFormateado"
            
            binding.tvTotalAhorros.text = format.format(ahorrosData.totalAhorros)
            binding.tvDesgloseAhorros.text = "Cuentas: ${format.format(ahorrosData.ahorroEnCuenta)} • Alcancía/Efectivo: ${format.format(ahorrosData.ahorroEnAlcancia)}"

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
        
        binding.tvSubtextGastos.text = if (countGastosMes == 1) "1 registro activo" else "$countGastosMes registros activos"
        binding.tvSubtextGastosFijos.text = "$countGF recurrentes"
        binding.tvSubtextDeudas.text = if (countDeudas > 0) "$countDeudas activas" else "Bajo control"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
