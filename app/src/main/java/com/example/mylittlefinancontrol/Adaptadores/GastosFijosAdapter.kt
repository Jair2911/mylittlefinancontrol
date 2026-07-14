package com.example.mylittlefinancontrol.Adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.ItemGastoFijoBinding

class GastosFijosAdapter(
    private var listaGastos: List<GastoFijo>,
    private val onEditarClick: (GastoFijo) -> Unit,
    private val onEliminarClick: (GastoFijo) -> Unit
) : RecyclerView.Adapter<GastosFijosAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemGastoFijoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGastoFijoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gasto = listaGastos[position]
        holder.binding.apply {
            tvConcepto.text = gasto.concepto
            tvCategoria.text = gasto.categoria
            tvMonto.text = "$${String.format("%.2f", gasto.monto)}"
            tvPagoInfo.text = "Día ${gasto.diaPago} - ${gasto.periodo}"

            ivEditar.setOnClickListener { onEditarClick(gasto) }
            ivEliminar.setOnClickListener { onEliminarClick(gasto) }
        }
    }

    override fun getItemCount() = listaGastos.size

    fun actualizarLista(nuevaLista: List<GastoFijo>) {
        listaGastos = nuevaLista
        notifyDataSetChanged()
    }
}
