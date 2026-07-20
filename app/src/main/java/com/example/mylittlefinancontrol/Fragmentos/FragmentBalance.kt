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
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.DialogAgregarDeudaBinding
import com.example.mylittlefinancontrol.databinding.FragmentBalanceBinding
import java.text.NumberFormat
import java.util.*

class FragmentBalance : Fragment() {

    private var _binding: FragmentBalanceBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var adapter: DeudasAdapter
    private var listaDeudas = mutableListOf<Deuda>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBalanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

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
        dialogBinding.autoTipoDeuda.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, tipos))

        deudaExistente?.let {
            dialogBinding.tvTituloDialogo.text = "Editar Deuda"
            dialogBinding.autoTipoDeuda.setText(it.tipo, false)
            dialogBinding.etConcepto.setText(it.concepto)
            dialogBinding.etDescripcion.setText(it.descripcion)
            dialogBinding.etTotalDeuda.setText(it.totalDeuda.toString())
            dialogBinding.etTotalPagado.setText(it.totalPagado.toString())
        }

        val alertDialog = builder.create()
        // 🔥 ESTA ES LA LÍNEA MÁGICA: Borra el marco cuadrado que Android pone por defecto
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        dialogBinding.btnGuardar.setOnClickListener {
            val tipo = dialogBinding.autoTipoDeuda.text.toString()
            val concepto = dialogBinding.etConcepto.text.toString()
            val descripcion = dialogBinding.etDescripcion.text.toString()
            val total = dialogBinding.etTotalDeuda.text.toString().toDoubleOrNull() ?: 0.0
            val pagado = dialogBinding.etTotalPagado.text.toString().toDoubleOrNull() ?: 0.0

            if (tipo.isNotEmpty() && concepto.isNotEmpty() && total > 0) {
                if (deudaExistente == null) {
                    db.insertDeuda(Deuda("", tipo, concepto, descripcion, total, pagado))
                } else {
                    deudaExistente.tipo = tipo
                    deudaExistente.concepto = concepto
                    deudaExistente.descripcion = descripcion
                    deudaExistente.totalDeuda = total
                    deudaExistente.totalPagado = pagado
                    db.updateDeuda(deudaExistente)
                }
                cargarDeudas()
                alertDialog.dismiss()
            } else {
                Toast.makeText(mContext, "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }
        dialogBinding.btnCancelar.setOnClickListener { alertDialog.dismiss() }
    }
    private fun cargarDeudas() {
        listaDeudas = db.getAllDeudas().toMutableList()
        adapter.actualizarLista(listaDeudas)
        
        val totalRestante = listaDeudas.sumOf { it.totalDeuda - it.totalPagado }
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        binding.tvTotalDeudasMonto.text = format.format(totalRestante)
    }

    private fun eliminarDeuda(deuda: Deuda) {
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Deuda")
            .setMessage("¿Estás seguro?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteDeuda(deuda.id)
                cargarDeudas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
