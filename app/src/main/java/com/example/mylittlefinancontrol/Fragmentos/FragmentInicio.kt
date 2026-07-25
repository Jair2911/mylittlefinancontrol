package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.databinding.FragmentInicioBinding
import java.text.NumberFormat
import java.util.*

class FragmentInicio : Fragment() {
    private lateinit var binding: FragmentInicioBinding
    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper

    private var totalGastosFijos = 0.0
    private var totalDeudasRestante = 0.0
    private var ingresosMes = 0.0
    private var gastosMes = 0.0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

        cargarDatosLocales()
        setupClickListeners()
    }

    private fun cargarDatosLocales() {
        // 1. Obtener Gastos Fijos localmente
        val listaGF = db.getAllGastosFijos()
        totalGastosFijos = listaGF.sumOf { it.monto }

        // 2. Obtener Deudas localmente
        val listaD = db.getAllDeudas()
        totalDeudasRestante = listaD.sumOf { it.totalDeuda - it.totalPagado }

        // 3. Calcular Movimientos del Mes (SQLite)
        calcularMovimientosDelMes()
    }

    private fun setupClickListeners() {
        val mainActivity = activity as? MainActivity

        binding.cardBalanceGeneral.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardIngresos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardGastos.setOnClickListener { mainActivity?.verFragmentoMovimientos() }
        binding.cardGastosFijos.setOnClickListener { mainActivity?.verFragmentoGastosFijos() }
        binding.cardDeudas.setOnClickListener { mainActivity?.verFragmentoDeudas() }
    }

    private fun calcularMovimientosDelMes() {
        val calendar = Calendar.getInstance()
        val mesActual = calendar.get(Calendar.MONTH)
        val anioActual = calendar.get(Calendar.YEAR)

        val todosLosMovimientos = db.getAllNotas()
        ingresosMes = 0.0
        gastosMes = 0.0

        for (mov in todosLosMovimientos) {
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
            } catch (e: Exception) {}
        }
        
        if (todosLosMovimientos.isNotEmpty()) {
            val ultimo = todosLosMovimientos[0]
            binding.tvUltimoRegDesc.text = "${ultimo.descripcion} - $${ultimo.valor}"
            binding.tvUltimoRegFecha.text = ultimo.fecha
        } else {
            binding.tvUltimoRegDesc.text = "Sin registros"
            binding.tvUltimoRegFecha.text = "-"
        }
        actualizarInterfaz()
    }

    private fun actualizarInterfaz() {
        if (!isAdded) return
        
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        
        binding.tvIngresosMes.text = format.format(ingresosMes)
        binding.tvGastosMes.text = format.format(gastosMes)
        binding.tvTotalGastosFijos.text = format.format(totalGastosFijos)
        binding.tvTotalDeudas.text = format.format(totalDeudasRestante)

        // Balance = Ingresos - (Gastos Variables + Gastos Fijos)
        val balance = ingresosMes - (gastosMes + totalGastosFijos)
        binding.tvBalanceGeneral.text = format.format(balance)
        
        val colorBalance = if (balance < 0) 
            com.example.mylittlefinancontrol.R.color.rojo_alerta
        else 
            com.example.mylittlefinancontrol.R.color.verde_oscuro
            
        binding.tvBalanceGeneral.setTextColor(mContext.getColor(colorBalance))
    }
}
