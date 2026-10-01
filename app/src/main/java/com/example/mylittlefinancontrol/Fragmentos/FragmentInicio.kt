package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mylittlefinancontrol.DetallePeriodoActivity
import com.example.mylittlefinancontrol.GeminiAdvisorDialogFragment
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.FragmentInicioBinding
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

        cargarDatosLocales()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        cargarDatosLocales()
    }

    private fun cargarDatosLocales() {
        val listaGF = db.getAllGastosFijos()
        totalGastosFijos = listaGF.sumOf { it.monto }

        val listaD = db.getAllDeudas()
        totalDeudasRestante = listaD.sumOf { (it.totalDeuda - it.totalPagado).coerceAtLeast(0.0) }

        calcularMovimientosDelMes()
        cargarPanelResumenesGrid()
    }

    private fun setupClickListeners() {
        val mainActivity = activity as? MainActivity

        binding.cardBalanceGeneral.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardIngresos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardGastos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardGastosFijos.setOnClickListener { mainActivity?.verFragmentoGastosFijos() }
        binding.cardDeudas.setOnClickListener { mainActivity?.verFragmentoDeudas() }

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
                val f = if (mov.fecha.isNotBlank()) mov.fecha else mov.fechaCreacion
                val partes = f.split(" ")[0].split("-")
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

        actualizarInterfaz()
    }

    private fun cargarPanelResumenesGrid() {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        val periodos = listOf("SEMANAL", "MENSUAL", "SEMESTRAL", "ANUAL")
        for (p in periodos) {
            val rangos = calcularFechasPeriodo(p)
            val rangeActual = rangos.first
            val rangeAnt = rangos.second

            val resAct = db.getResumenPorRangoFechas(rangeActual.first, rangeActual.second)
            val resAnt = db.getResumenPorRangoFechas(rangeAnt.first, rangeAnt.second)

            val gastosAct = resAct.totalGastos
            val gastosAnt = resAnt.totalGastos

            val varPct = if (gastosAnt > 0) {
                ((gastosAct - gastosAnt) / gastosAnt) * 100.0
            } else {
                0.0
            }

            val varTxt = if (varPct > 0) "📈 +${varPct.toInt()}% vs ant." else if (varPct < 0) "📉 ${varPct.toInt()}% vs ant." else "📊 Estables vs ant."

            when (p) {
                "SEMANAL" -> {
                    binding.tvBalanceSemanal.text = format.format(resAct.balanceNeto)
                    binding.tvTendenciaSemanal.text = varTxt
                }
                "MENSUAL" -> {
                    binding.tvBalanceMensual.text = format.format(resAct.balanceNeto)
                    binding.tvTendenciaMensual.text = varTxt
                }
                "SEMESTRAL" -> {
                    binding.tvBalanceSemestral.text = format.format(resAct.balanceNeto)
                    binding.tvTendenciaSemestral.text = varTxt
                }
                "ANUAL" -> {
                    binding.tvBalanceAnual.text = format.format(resAct.balanceNeto)
                    binding.tvTendenciaAnual.text = varTxt
                }
            }
        }
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
        val balanceTotal = ingresosTotales - gastosTotales

        if (esIncognito) {
            binding.tvBalanceGeneral.text = "••••••••"
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
