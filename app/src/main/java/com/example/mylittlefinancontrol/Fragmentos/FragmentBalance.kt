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
import com.example.mylittlefinancontrol.Adaptadores.DeudasAdapter
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.DialogAgregarDeudaBinding
import com.example.mylittlefinancontrol.databinding.FragmentBalanceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class FragmentBalance : Fragment() {

    private var _binding: FragmentBalanceBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mContext: Context
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var adapter: DeudasAdapter
    private val listaDeudas = mutableListOf<Deuda>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBalanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        setupRecyclerView()
        cargarDeudas()

        binding.fabAgregarDeuda.setOnClickListener {
            mostrarDialogoDeuda(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = DeudasAdapter(listaDeudas,
            onEditarClick = { deuda -> mostrarDialogoDeuda(deuda) },
            onEliminarClick = { deuda -> eliminarDeuda(deuda) }
        )
        binding.rvDeudas.layoutManager = LinearLayoutManager(mContext)
        binding.rvDeudas.adapter = adapter
    }

    private fun mostrarDialogoDeuda(deudaExistente: Deuda?) {
        val builder = AlertDialog.Builder(mContext)
        val dialogBinding = DialogAgregarDeudaBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)

        val tipos = arrayOf("Tarjeta de crédito", "Préstamo", "Otro")
        val adapterTipos = ArrayAdapter(mContext, android.R.layout.simple_list_item_1, tipos)
        dialogBinding.autoTipoDeuda.setAdapter(adapterTipos)

        deudaExistente?.let {
            dialogBinding.tvTituloDialogo.text = "Editar Deuda"
            dialogBinding.autoTipoDeuda.setText(it.tipo, false)
            dialogBinding.etConcepto.setText(it.concepto)
            dialogBinding.etDescripcion.setText(it.descripcion)
            dialogBinding.etTotalDeuda.setText(it.totalDeuda.toString())
            dialogBinding.etTotalPagado.setText(it.totalPagado.toString())
        }

        val alertDialog = builder.create()
        alertDialog.show()

        dialogBinding.btnGuardar.setOnClickListener {
            val tipo = dialogBinding.autoTipoDeuda.text.toString()
            val concepto = dialogBinding.etConcepto.text.toString()
            val descripcion = dialogBinding.etDescripcion.text.toString()
            val total = dialogBinding.etTotalDeuda.text.toString().toDoubleOrNull() ?: 0.0
            val pagado = dialogBinding.etTotalPagado.text.toString().toDoubleOrNull() ?: 0.0

            if (tipo.isNotEmpty() && concepto.isNotEmpty() && total > 0) {
                val id = deudaExistente?.id ?: FirebaseDatabase.getInstance().getReference("Deudas").push().key ?: ""
                val deuda = Deuda(id, tipo, concepto, descripcion, total, pagado)

                FirebaseDatabase.getInstance().getReference("Deudas")
                    .child(firebaseAuth.uid!!)
                    .child(id)
                    .setValue(deuda)
                    .addOnSuccessListener {
                        Toast.makeText(mContext, "Deuda guardada", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
            } else {
                Toast.makeText(mContext, "Por favor complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancelar.setOnClickListener { alertDialog.dismiss() }
    }

    private fun cargarDeudas() {
        val uid = firebaseAuth.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Deudas").child(uid)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaDeudas.clear()
                var totalRestante = 0.0
                for (ds in snapshot.children) {
                    val deuda = ds.getValue(Deuda::class.java)
                    if (deuda != null) {
                        listaDeudas.add(deuda)
                        totalRestante += (deuda.totalDeuda - deuda.totalPagado)
                    }
                }
                adapter.actualizarLista(listaDeudas)
                
                val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                binding.tvTotalDeudasMonto.text = format.format(totalRestante)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun eliminarDeuda(deuda: Deuda) {
        val uid = firebaseAuth.uid ?: return
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Deuda")
            .setMessage("¿Estás seguro de eliminar esta deuda?")
            .setPositiveButton("Eliminar") { _, _ ->
                FirebaseDatabase.getInstance().getReference("Deudas")
                    .child(uid)
                    .child(deuda.id)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(mContext, "Deuda eliminada", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
