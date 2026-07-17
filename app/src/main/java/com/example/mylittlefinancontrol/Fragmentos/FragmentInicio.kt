package com.example.mylittlefinancontrol.Fragmentos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mylittlefinancontrol.MainActivity
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.databinding.FragmentInicioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class FragmentInicio : Fragment() {
    private lateinit var binding: FragmentInicioBinding
    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var firebaseAuth: FirebaseAuth

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
        firebaseAuth = FirebaseAuth.getInstance()
        db = NotasDatabaseHelper(mContext)

        cargarDatosFinancieros()
        setupClickListeners()
    }

    private fun cargarDatosFinancieros() {
        val uid = firebaseAuth.uid ?: return
        val database = FirebaseDatabase.getInstance()

        // 1. Consultar Gastos Fijos desde Firebase
        database.getReference("GastosFijos").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    totalGastosFijos = 0.0
                    for (ds in snapshot.children) {
                        val gf = ds.getValue(GastoFijo::class.java)
                        if (gf != null) totalGastosFijos += gf.monto
                    }
                    actualizarInterfaz()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // 2. Consultar Deudas desde Firebase
        database.getReference("Deudas").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    totalDeudasRestante = 0.0
                    for (ds in snapshot.children) {
                        val d = ds.getValue(Deuda::class.java)
                        if (d != null) totalDeudasRestante += (d.totalDeuda - d.totalPagado)
                    }
                    actualizarInterfaz()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // 3. Movimientos locales (SQLite)
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

        val balance = ingresosMes - (gastosMes + totalGastosFijos)
        binding.tvBalanceGeneral.text = format.format(balance)
        
        val colorBalance = if (balance < 0) 
            com.example.mylittlefinancontrol.R.color.accent_alerta 
        else 
            com.example.mylittlefinancontrol.R.color.primary
            
        binding.tvBalanceGeneral.setTextColor(mContext.getColor(colorBalance))
    }
}
