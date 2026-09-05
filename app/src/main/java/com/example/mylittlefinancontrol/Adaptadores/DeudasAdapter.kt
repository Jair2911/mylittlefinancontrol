package com.example.mylittlefinancontrol.Adaptadores

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.ItemDeudaBinding
import java.text.NumberFormat
import java.util.Locale

class DeudasAdapter(
    private var listaDeudas: List<Deuda>,
    private val onEditarClick: (Deuda) -> Unit,
    private val onEliminarClick: (Deuda) -> Unit,
    private val onAbonarClick: (Deuda) -> Unit
) : RecyclerView.Adapter<DeudasAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDeudaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeudaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deuda = listaDeudas[position]
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val context = holder.itemView.context

        holder.binding.apply {
            tvConcepto.text = deuda.concepto
            tvTipo.text = deuda.tipo
            
            if (deuda.descripcion.isNotEmpty()) {
                llNotaContainer.visibility = View.VISIBLE
                tvDescripcion.text = "Nota: \"${deuda.descripcion}\""
            } else {
                llNotaContainer.visibility = View.GONE
            }

            val tipoLower = deuda.tipo.lowercase()
            when {
                tipoLower.contains("tarjeta") -> {
                    flIconoDeuda.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_low))
                    ivIconoDeuda.setImageResource(R.drawable.ic_credit_card)
                    ivIconoDeuda.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_emerald))
                }
                tipoLower.contains("auto") || tipoLower.contains("vehic") -> {
                    flIconoDeuda.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_low))
                    ivIconoDeuda.setImageResource(R.drawable.ic_directions_car)
                    ivIconoDeuda.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_emerald))
                }
                tipoLower.contains("personal") -> {
                    flIconoDeuda.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                    ivIconoDeuda.setImageResource(R.drawable.ic_person)
                    ivIconoDeuda.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_secondary_container))
                }
                else -> {
                    flIconoDeuda.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high))
                    ivIconoDeuda.setImageResource(R.drawable.ic_account_balance_wallet)
                    ivIconoDeuda.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }
            
            val restante = (deuda.totalDeuda - deuda.totalPagado).coerceAtLeast(0.0)
            tvValorRestanteTop.text = format.format(restante)
            
            val progresoPct = if (deuda.totalDeuda > 0) {
                ((deuda.totalPagado / deuda.totalDeuda) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            // Badge Estado
            when {
                restante <= 0.0 -> {
                    tvBadgeEstado.text = "PAGADA"
                    tvBadgeEstado.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_low))
                    tvBadgeEstado.setTextColor(ContextCompat.getColor(context, R.color.primary_emerald))
                    tvLabelMontoTop.text = "Completado"
                }
                progresoPct > 0 -> {
                    tvBadgeEstado.text = "AL DÍA"
                    tvBadgeEstado.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_low))
                    tvBadgeEstado.setTextColor(ContextCompat.getColor(context, R.color.primary_emerald))
                    tvLabelMontoTop.text = "Pendiente"
                }
                else -> {
                    tvBadgeEstado.text = "EN CURSO"
                    tvBadgeEstado.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.debt_amber_bg))
                    tvBadgeEstado.setTextColor(ContextCompat.getColor(context, R.color.debt_amber))
                    tvLabelMontoTop.text = "Saldo total"
                }
            }

            tvPagadoLabel.text = "Pagado: ${format.format(deuda.totalPagado)} ($progresoPct%)"
            tvRestanteLabel.text = "Restante: ${format.format(restante)}"
            
            if (deuda.fechaInicio.isNotEmpty() || deuda.fechaFin.isNotEmpty()) {
                tvFechas.visibility = View.VISIBLE
                tvFechas.text = "Plazo: ${deuda.fechaInicio} - ${deuda.fechaFin}"
            } else {
                tvFechas.visibility = View.GONE
            }

            pbDeuda.max = 100
            pbDeuda.progress = progresoPct

            ivEditar.setOnClickListener { onEditarClick(deuda) }
            ivEliminar.setOnClickListener { onEliminarClick(deuda) }
            btnAbonarCuota.setOnClickListener { onAbonarClick(deuda) }
        }
    }

    override fun getItemCount() = listaDeudas.size

    fun actualizarLista(nuevaLista: List<Deuda>) {
        listaDeudas = nuevaLista
        notifyDataSetChanged()
    }
}
