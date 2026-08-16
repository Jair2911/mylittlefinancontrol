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
import com.example.mylittlefinancontrol.Modelos.GastoFijo
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
    private val listaGastosFijos = mutableListOf<GastoFijo>()
    private var gastoFijoSeleccionado: GastoFijo? = null

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
        listaGastosFijos.clear()
        listaGastosFijos.addAll(db.getAllGastosFijos())
    }

    private fun mostrarDialogoMovimiento(movimientoExistente: Movimiento?) {
        val dialogBinding = DialogAgregarMovimientoBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(mContext).setView(dialogBinding.root)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cargarDeudasLocales()
        deudaSeleccionada = null
        gastoFijoSeleccionado = null

        val opciones = arrayOf("Gasto", "Ingreso", "Otro")
        dialogBinding.autoTipoMov.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, opciones))

        val nombresDeudas = listaDeudas.map { "${it.concepto} (Resta: $${it.totalDeuda - it.totalPagado})" }
        dialogBinding.autoDeudas.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, nombresDeudas))

        val nombresGastos = listaGastosFijos.map { "${it.concepto} ($${it.monto})" }
        dialogBinding.autoGastosFijos.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, nombresGastos))

        val opcionesQuincena = arrayOf("1ª Quincena", "2ª Quincena")
        dialogBinding.autoQuincena.setAdapter(ArrayAdapter(mContext, android.R.layout.simple_list_item_1, opcionesQuincena))
        
        // Selección de quincena por defecto basada en el día actual
        val diaActual = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val defaultQ = if (diaActual <= 15) "1ª Quincena" else "2ª Quincena"
        dialogBinding.autoQuincena.setText(defaultQ, false)

        if (movimientoExistente != null) {
            dialogBinding.tvTituloDialogo.text = "Editar Movimiento"
            dialogBinding.etDescripcion.setText(movimientoExistente.descripcion)
            dialogBinding.etValor.setText(movimientoExistente.valor.toString())
            dialogBinding.autoTipoMov.setText(movimientoExistente.tipo_reg.lowercase().capitalize(), false)
            
            // Si es un pago de deuda o gasto fijo, podrías querer manejarlo, pero por simplicidad permitimos editar tipo
            if (movimientoExistente.tipo_reg == "PAGO DEUDA" || movimientoExistente.tipo_reg == "GASTO FIJO") {
                dialogBinding.cbEsPagoDeuda.visibility = View.GONE 
                dialogBinding.cbEsGastoFijo.visibility = View.GONE
            }
        }

        dialogBinding.cbEsPagoDeuda.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.tilSeleccionDeuda.visibility = if (isChecked) View.VISIBLE else View.GONE
            dialogBinding.tilTipoReg.visibility = if (isChecked || dialogBinding.cbEsGastoFijo.isChecked) View.GONE else View.VISIBLE
            
            // Ocultar el otro checkbox para que sean mutuamente excluyentes
            dialogBinding.cbEsGastoFijo.visibility = if (isChecked) View.GONE else View.VISIBLE
            
            if (isChecked) {
                dialogBinding.cbEsGastoFijo.isChecked = false
                gastoFijoSeleccionado = null
            }
        }

        dialogBinding.cbEsGastoFijo.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.tilSeleccionGastoFijo.visibility = if (isChecked) View.VISIBLE else View.GONE
            dialogBinding.tilTipoReg.visibility = if (isChecked || dialogBinding.cbEsPagoDeuda.isChecked) View.GONE else View.VISIBLE
            
            // Ocultar el otro checkbox
            dialogBinding.cbEsPagoDeuda.visibility = if (isChecked) View.GONE else View.VISIBLE
            
            if (isChecked) {
                dialogBinding.cbEsPagoDeuda.isChecked = false
                deudaSeleccionada = null
            }
        }

        dialogBinding.autoDeudas.setOnItemClickListener { _, _, pos, _ -> 
            deudaSeleccionada = listaDeudas[pos] 
        }

        dialogBinding.autoGastosFijos.setOnItemClickListener { _, _, pos, _ ->
            gastoFijoSeleccionado = listaGastosFijos[pos]
            dialogBinding.tilSeleccionQuincena.visibility = if (gastoFijoSeleccionado?.periodo == "Quincenal") View.VISIBLE else View.GONE
            dialogBinding.etDescripcion.setText("PAGO: ${gastoFijoSeleccionado?.concepto}")
            dialogBinding.etValor.setText(gastoFijoSeleccionado?.monto?.toInt().toString())
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
                val fecha = Constantes.formatoFecha(System.currentTimeMillis()) // yyyy-MM-dd
                val mesAnio = fecha.substring(0, 7) // yyyy-MM

                if (dialogBinding.cbEsPagoDeuda.isChecked && deudaSeleccionada != null) {
                    deudaSeleccionada!!.totalPagado += valor
                    db.updateDeuda(deudaSeleccionada!!)
                    db.insertNota(Movimiento(0, desc.uppercase(), valor, "PAGO DEUDA", fecha))
                } else if (dialogBinding.cbEsGastoFijo.isChecked && gastoFijoSeleccionado != null) {
                    val gf = gastoFijoSeleccionado!!
                    var quincena: Int? = null
                    if (gf.periodo == "Quincenal") {
                        val qPos = dialogBinding.autoQuincena.text.toString()
                        if (qPos == "1ª Quincena") {
                            gf.ultimoPagoQ1 = mesAnio
                            quincena = 1
                        } else if (qPos == "2ª Quincena") {
                            gf.ultimoPagoQ2 = mesAnio
                            quincena = 2
                        }
                    } else {
                        gf.ultimoPagoQ1 = mesAnio // Para mensual usamos Q1 como indicador principal
                        quincena = 0
                    }
                    db.updateGastoFijo(gf)
                    db.insertNota(Movimiento(0, desc.uppercase(), valor, "GASTO FIJO", fecha, gf.id.toInt(), quincena))
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
