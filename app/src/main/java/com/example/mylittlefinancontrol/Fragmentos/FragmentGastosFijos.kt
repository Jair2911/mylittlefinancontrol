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
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.DialogAgregarGastoFijoBinding
import com.example.mylittlefinancontrol.databinding.FragmentGastosFijosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FragmentGastosFijos : Fragment() {

    private lateinit var binding: FragmentGastosFijosBinding
    private lateinit var mContext: Context
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var adapter: GastosFijosAdapter
    private val listaGastos = mutableListOf<GastoFijo>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGastosFijosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

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

        // CORRECCIÓN: Usar android.R.layout para recursos del sistema
        val adapterCategorias = ArrayAdapter(mContext, android.R.layout.simple_list_item_1, categorias)
        dialogBinding.autoCategoria.setAdapter(adapterCategorias)

        val adapterPeriodos = ArrayAdapter(mContext, android.R.layout.simple_list_item_1, periodos)
        dialogBinding.autoPeriodo.setAdapter(adapterPeriodos)

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
                val id = gastoExistente?.id ?: FirebaseDatabase.getInstance().getReference("GastosFijos").push().key ?: ""
                val gasto = GastoFijo(id, concepto, categoria, monto, diaPago, periodo)
                
                FirebaseDatabase.getInstance().getReference("GastosFijos")
                    .child(firebaseAuth.uid!!)
                    .child(id)
                    .setValue(gasto)
                    .addOnSuccessListener {
                        Toast.makeText(mContext, "Gasto guardado", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
            } else {
                Toast.makeText(mContext, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancelar.setOnClickListener { alertDialog.dismiss() }
    }

    private fun cargarGastosFijos() {
        val uid = firebaseAuth.uid
        if (uid != null) {
            val ref = FirebaseDatabase.getInstance().getReference("GastosFijos").child(uid)
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaGastos.clear()
                    var total = 0.0
                    for (ds in snapshot.children) {
                        val gasto = ds.getValue(GastoFijo::class.java)
                        if (gasto != null) {
                            listaGastos.add(gasto)
                            total += gasto.monto
                        }
                    }
                    adapter.actualizarLista(listaGastos)
                    binding.tvTotalMonto.text = "$${String.format("%.2f", total)}"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun eliminarGastoFijo(gasto: GastoFijo) {
        val uid = firebaseAuth.uid
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("GastosFijos")
                .child(uid)
                .child(gasto.id)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(mContext, "Gasto eliminado", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
