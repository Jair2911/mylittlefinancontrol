package com.example.mylittlefinancontrol

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.databinding.ActivityAgregarMovimientoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class AgregarMovimientoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgregarMovimientoBinding
    private lateinit var db: NotasDatabaseHelper
    private lateinit var firebaseAuth: FirebaseAuth
    private val listaDeudas = mutableListOf<Deuda>()
    private var deudaSeleccionada: Deuda? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarMovimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotasDatabaseHelper(this)
        firebaseAuth = FirebaseAuth.getInstance()

        setupSpinners()
        cargarDeudasDesdeFirebase()

        binding.cbEsPagoDeuda.setOnCheckedChangeListener { _, isChecked ->
            binding.tilSeleccionDeuda.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.tilTipoReg.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        binding.autoDeudas.setOnItemClickListener { _, _, position, _ ->
            deudaSeleccionada = listaDeudas[position]
        }

        binding.ivGuardarMovimiento.setOnClickListener {
            validarYGuardar()
        }
    }

    private fun setupSpinners() {
        val opciones = arrayOf("Gasto", "Ingreso", "Otro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, opciones)
        binding.autoTipoMov.setAdapter(adapter)
    }

    private fun cargarDeudasDesdeFirebase() {
        val uid = firebaseAuth.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Deudas").child(uid)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaDeudas.clear()
                val nombresDeudas = mutableListOf<String>()
                for (ds in snapshot.children) {
                    val deuda = ds.getValue(Deuda::class.java)
                    if (deuda != null) {
                        listaDeudas.add(deuda)
                        nombresDeudas.add("${deuda.concepto} (Resta: $${deuda.totalDeuda - deuda.totalPagado})")
                    }
                }
                val adapter = ArrayAdapter(this@AgregarMovimientoActivity, android.R.layout.simple_list_item_1, nombresDeudas)
                binding.autoDeudas.setAdapter(adapter)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun validarYGuardar() {
        val descripcion = binding.etDescripcion.text.toString()
        val valorStr = binding.etValor.text.toString()
        val esPagoDeuda = binding.cbEsPagoDeuda.isChecked
        val fecha = Constantes.formatoFecha(Constantes.obtenerTiempoD())

        if (descripcion.isEmpty() || valorStr.isEmpty()) {
            Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val valor = valorStr.toInt()

        if (esPagoDeuda) {
            if (deudaSeleccionada == null) {
                Toast.makeText(this, "Seleccione una deuda", Toast.LENGTH_SHORT).show()
                return
            }
            // Actualizar deuda en Firebase
            actualizarDeudaEnFirebase(deudaSeleccionada!!, valor)
            guardarMovimento(descripcion.uppercase(), valor, "PAGO DEUDA", fecha)
        } else {
            val tipo = binding.autoTipoMov.text.toString()
            guardarMovimento(descripcion.uppercase(), valor, tipo.uppercase(), fecha)
        }
    }

    private fun actualizarDeudaEnFirebase(deuda: Deuda, abono: Int) {
        val uid = firebaseAuth.uid ?: return
        val nuevoPagado = deuda.totalPagado + abono
        FirebaseDatabase.getInstance().getReference("Deudas")
            .child(uid)
            .child(deuda.id)
            .child("totalPagado")
            .setValue(nuevoPagado)
    }

    private fun guardarMovimento(descripcion: String, valor: Int, tipo_reg: String, fecha: String) {
        val movimiento = Movimiento(0, descripcion, valor, tipo_reg, fecha)
        db.insertNota(movimiento)
        Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_SHORT).show()
        finish()
    }
}
