package com.example.mylittlefinancontrol.Adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mylittlefinancontrol.Modelos.GastoFijo
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.ItemGastoFijoBinding
import java.text.NumberFormat
import java.util.*

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
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        holder.binding.apply {
            tvConcepto.text = "Concepto: ${gasto.concepto}"
            tvCategoria.text = "Categoria: ${gasto.categoria}"
            tvMonto.text = "Monto: ${format.format(gasto.monto)}"
            tvPagoInfo.text = "Día ${gasto.diaPago} - ${gasto.periodo}"

            // Estado de pago
            val cal = Calendar.getInstance()
            val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

            val colorPagado = holder.itemView.context.getColor(com.example.mylittlefinancontrol.R.color.verde_oscuro)
            val colorPendiente = holder.itemView.context.getColor(com.example.mylittlefinancontrol.R.color.rojo_alerta)

            if (gasto.periodo == "Quincenal") {
                tvEstadoQ2.visibility = View.VISIBLE
                
                // Q1
                if (gasto.ultimoPagoQ1 == mesAnioActual) {
                    tvEstadoQ1.text = "Q1 PAGADA"
                    tvEstadoQ1.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPagado)
                } else {
                    tvEstadoQ1.text = "Q1 PENDIENTE"
                    tvEstadoQ1.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPendiente)
                }

                // Q2
                if (gasto.ultimoPagoQ2 == mesAnioActual) {
                    tvEstadoQ2.text = "Q2 PAGADA"
                    tvEstadoQ2.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPagado)
                } else {
                    tvEstadoQ2.text = "Q2 PENDIENTE"
                    tvEstadoQ2.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPendiente)
                }
            } else {
                tvEstadoQ2.visibility = View.GONE
                if (gasto.ultimoPagoQ1 == mesAnioActual) {
                    tvEstadoQ1.text = "PAGADO"
                    tvEstadoQ1.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPagado)
                } else {
                    tvEstadoQ1.text = "PENDIENTE"
                    tvEstadoQ1.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPendiente)
                }
            }

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
