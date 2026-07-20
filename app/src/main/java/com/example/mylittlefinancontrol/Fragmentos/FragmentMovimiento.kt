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
import com.example.mylittlefinancontrol.Constantes
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.NotasAdaptador
import com.example.mylittlefinancontrol.NotasDatabaseHelper
import com.example.mylittlefinancontrol.databinding.DialogAgregarMovimientoBinding
import com.example.mylittlefinancontrol.databinding.FragmentMovimientoBinding
import java.text.NumberFormat
import java.util.*

class FragmentMovimiento : Fragment() {

    private lateinit var binding: FragmentMovimientoBinding
    private lateinit var mContext: Context
    private lateinit var db: NotasDatabaseHelper
    private lateinit var notasAdaptador: NotasAdaptador

    private var mesSeleccionado: Int = 0
    private var anioSeleccionado: Int = 0
    private val listaDeudas = mutableListOf<Deuda>()
    private var deudaSeleccionada: Deuda? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMovimientoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = NotasDatabaseHelper(mContext)

        setupFiltros()
        setupRecyclerView()
        cargarDeudasLocales()

        binding.FABAgregarNota.setOnClickListener {
            mostrarDialogoMovimiento(null)
        }
    }

    private fun setupFiltros() {
        val calendar = Calendar.getInstance()
        mesSeleccionado = calendar.get(Calendar.MONTH)
        anioSeleccionado = calendar.get(Calendar.YEAR)

        val meses = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        binding.autoMes.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, meses))
        binding.autoMes.setText(meses[mesSeleccionado], false)

        val anios = Array(5) { (anioSeleccionado - 2 + it).toString() }
        binding.autoAnio.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, anios))
        binding.autoAnio.setText(anioSeleccionado.toString(), false)

        binding.autoMes.setOnItemClickListener { _, _, position, _ ->
            mesSeleccionado = position
            actualizarListaFiltrada()
        }
        binding.autoAnio.setOnItemClickListener { _, _, _, _ ->
            anioSeleccionado = binding.autoAnio.text.toString().toInt()
            actualizarListaFiltrada()
        }
    }

    private fun setupRecyclerView() {
        notasAdaptador = NotasAdaptador(
            mutableListOf(),
            mContext,
            onEditarClick = { mov -> mostrarDialogoMovimiento(mov) },
            onEliminarClick = { mov -> eliminarMovimiento(mov) }
        )
        binding.notasRv.layoutManager = LinearLayoutManager(mContext)
        binding.notasRv.adapter = notasAdaptador
        actualizarListaFiltrada()
    }

    private fun actualizarListaFiltrada() {
        val todos = db.getAllNotas()
        val filtrados = todos.filter { mov ->
            try {
                val partes = mov.fecha.split("-") // yyyy-MM-dd
                val anio = partes[0].toInt()
                val mes = partes[1].toInt() - 1
                anio == anioSeleccionado && mes == mesSeleccionado
            } catch (e: Exception) { false }
        }
        notasAdaptador.refrescarLista(filtrados)
        val total = filtrados.sumOf { it.valor.toDouble() }
        binding.tvTotalMovMonto.text = NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(total)
    }

    private fun cargarDeudasLocales() {
        listaDeudas.clear()
        listaDeudas.addAll(db.getAllDeudas())
    }

    private fun mostrarDialogoMovimiento(movimientoExistente: Movimiento?) {
        val dialogBinding = DialogAgregarMovimientoBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(mContext).setView(dialogBinding.root)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cargarDeudasLocales()

        val opciones = arrayOf("Gasto", "Ingreso", "Otro")
        dialogBinding.autoTipoMov.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, opciones))

        val nombresDeudas = listaDeudas.map { "${it.concepto} (Resta: $${it.totalDeuda - it.totalPagado})" }
        dialogBinding.autoDeudas.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, nombresDeudas))

        if (movimientoExistente != null) {
            dialogBinding.tvTituloDialogo.text = "Editar Movimiento"
            dialogBinding.etDescripcion.setText(movimientoExistente.descripcion)
            dialogBinding.etValor.setText(movimientoExistente.valor.toString())
            dialogBinding.autoTipoMov.setText(movimientoExistente.tipo_reg.lowercase().capitalize(), false)
            
            // Si es un pago de deuda, podrías querer manejarlo, pero por simplicidad permitimos editar tipo
            if (movimientoExistente.tipo_reg == "PAGO DEUDA") {
                dialogBinding.cbEsPagoDeuda.visibility = View.GONE // Ocultar para evitar inconsistencias al editar
            }
        }

        dialogBinding.cbEsPagoDeuda.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.tilSeleccionDeuda.visibility = if (isChecked) View.VISIBLE else View.GONE
            dialogBinding.tilTipoReg.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        dialogBinding.autoDeudas.setOnItemClickListener { _, _, pos, _ -> 
            deudaSeleccionada = listaDeudas[pos] 
        }

        dialogBinding.btnGuardar.setOnClickListener {
            val desc = dialogBinding.etDescripcion.text.toString()
            val valStr = dialogBinding.etValor.text.toString()
            if (desc.isEmpty() || valStr.isEmpty()) {
                Toast.makeText(mContext, "Por favor llene todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val valor = valStr.toInt()
            
            if (movimientoExistente == null) {
                // CREAR
                val fecha = Constantes.formatoFecha(System.currentTimeMillis())
                if (dialogBinding.cbEsPagoDeuda.isChecked && deudaSeleccionada != null) {
                    deudaSeleccionada!!.totalPagado += valor
                    db.updateDeuda(deudaSeleccionada!!)
                    db.insertNota(Movimiento(0, desc.uppercase(), valor, "PAGO DEUDA", fecha))
                } else {
                    val tipo = dialogBinding.autoTipoMov.text.toString()
                    db.insertNota(Movimiento(0, desc.uppercase(), valor, tipo.uppercase(), fecha))
                }
            } else {
                // EDITAR
                // Nota: Aquí podrías ajustar si el valor cambió y era pago de deuda, 
                // pero implementaremos la edición básica del movimiento.
                val nuevoMov = Movimiento(
                    movimientoExistente.id,
                    desc.uppercase(),
                    valor,
                    if (movimientoExistente.tipo_reg == "PAGO DEUDA") "PAGO DEUDA" else dialogBinding.autoTipoMov.text.toString().uppercase(),
                    movimientoExistente.fecha
                )
                db.updateMovimiento(nuevoMov)
            }
            
            alertDialog.dismiss()
            actualizarListaFiltrada()
        }
        dialogBinding.btnCancelar.setOnClickListener { alertDialog.dismiss() }
        alertDialog.show()
    }

    private fun eliminarMovimiento(movimiento: Movimiento) {
        AlertDialog.Builder(mContext)
            .setTitle("Eliminar Movimiento")
            .setMessage("¿Estás seguro de eliminar este movimiento?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteMovimiento(movimiento.id)
                actualizarListaFiltrada()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
