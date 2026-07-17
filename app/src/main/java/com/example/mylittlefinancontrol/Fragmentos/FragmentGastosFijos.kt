package com.example.mylittlefinancontrol.Fragmentos

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylittlefinancontrol.Adaptadores.GastosFijosAdapter
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.DialogAgregarGastoFijoBinding
import com.example.mylittlefinancontrol.databinding.FragmentGastosFijosBinding

class FragmentGastosFijos : Fragment() {

    private lateinit var binding: FragmentGastosFijosBinding
    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var adapter: GastosFijosAdapter
    private var listaGastos = mutableListOf<GastoFijo>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGastosFijosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

        setupRecyclerView()
        cargarGastosFijos()

        binding.fabAgregar.setOnClickListener {
            mostrarDialogoGastoFijo(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = GastosFijosAdapter(listaGastos, 
            onEditarClick = { gasto -> mostrarDialogoGastoFijo(gasto) },
            onEliminarClick = { gasto -> eliminarGastoFijo(gasto) }
        )
        binding.rvGastosFijos.layoutManager = LinearLayoutManager(mContext)
        binding.rvGastosFijos.adapter = adapter
    }

    private fun mostrarDialogoGastoFijo(gastoExistente: GastoFijo?) {
        val builder = AlertDialog.Builder(mContext)
        val dialogBinding = DialogAgregarGastoFijoBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)

        val categorias = arrayOf("Vivienda", "Servicios", "Transporte", "Seguros", "Suscripciones", "Personal", "Otro")
        val periodos = arrayOf("Mensual", "Quincenal")

        dialogBinding.autoCategoria.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, categorias))
        dialogBinding.autoPeriodo.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, periodos))

        gastoExistente?.let {
            dialogBinding.etConcepto.setText(it.concepto)
            dialogBinding.etMonto.setText(it.monto.toString())
            dialogBinding.etDiaPago.setText(it.diaPago)
            dialogBinding.autoCategoria.setText(it.categoria, false)
            dialogBinding.autoPeriodo.setText(it.periodo, false)
        }

        val alertDialog = builder.create()
        alertDialog.show()

        dialogBinding.btnGuardar.setOnClickListener {
            val concepto = dialogBinding.etConcepto.text.toString()
            val monto = dialogBinding.etMonto.text.toString().toDoubleOrNull() ?: 0.0
            val diaPago = dialogBinding.etDiaPago.text.toString()
            val categoria = dialogBinding.autoCategoria.text.toString()
            val periodo = dialogBinding.autoPeriodo.text.toString()

            if (concepto.isNotEmpty() && monto > 0 && diaPago.isNotEmpty()) {
                if (gastoExistente == null) {
                    db.insertGastoFijo(GastoFijo("", concepto, categoria, monto, diaPago, periodo))
                } else {
                    gastoExistente.concepto = concepto
                    gastoExistente.monto = monto
                    gastoExistente.diaPago = diaPago
                    gastoExistente.categoria = categoria
                    gastoExistente.periodo = periodo
                    db.updateGastoFijo(gastoExistente)
                }
                cargarGastosFijos()
                alertDialog.dismiss()
            } else {
                Toast.makeText(mContext, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
        dialogBinding.btnCancelar.setOnClickListener { alertDialog.dismiss() }
    }

    private fun cargarGastosFijos() {
        listaGastos = db.getAllGastosFijos().toMutableList()
        adapter.actualizarLista(listaGastos)
        val total = listaGastos.sumOf { it.monto }
        binding.tvTotalMonto.text = "$${String.format("%.2f", total)}"
    }

    private fun eliminarGastoFijo(gasto: GastoFijo) {
        db.deleteGastoFijo(gasto.id)
        cargarGastosFijos()
    }
}
