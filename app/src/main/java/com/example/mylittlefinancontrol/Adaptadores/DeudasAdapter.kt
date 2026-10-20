package com.example.mylittlefinancontrol.Adaptadores

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.ItemDeudaBinding
import java.text.NumberFormat
import java.util.Locale

object DeudaDiffCallback : DiffUtil.ItemCallback<Deuda>() {
    override fun areItemsTheSame(oldItem: Deuda, newItem: Deuda): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Deuda, newItem: Deuda): Boolean = oldItem == newItem
}

class DeudasAdapter(
    private val onEditarClick: (Deuda) -> Unit,
    private val onEliminarClick: (Deuda) -> Unit,
    private val onAbonarClick: (Deuda) -> Unit
) : ListAdapter<Deuda, DeudasAdapter.ViewHolder>(DeudaDiffCallback) {

    constructor(
        listaInicial: List<Deuda>,
        onEditarClick: (Deuda) -> Unit,
        onEliminarClick: (Deuda) -> Unit,
        onAbonarClick: (Deuda) -> Unit
    ) : this(onEditarClick, onEliminarClick, onAbonarClick) {
        submitList(listaInicial)
    }

    companion object {
        private val formatMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }

    class ViewHolder(val binding: ItemDeudaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeudaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deuda = getItem(position)
        val context = holder.itemView.context

        holder.binding.apply {
            tvConcepto.text = deuda.concepto

            val infoInteres = if (deuda.tasaInteres > 0) " · ${deuda.tasaInteres}% ${deuda.tipoInteres}" else ""
            val infoCuota = if (deuda.valorCuota > 0) {
                " (${deuda.cuotas}x · ${formatMoneda.format(deuda.valorCuota)}/mes)"
            } else if (deuda.cuotas > 0) {
                " (${deuda.cuotas} cuotas)"
            } else ""

            tvTipo.text = "${deuda.tipo}$infoInteres$infoCuota"
            
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
            val esPagadaCompletamente = restante <= 0.0 || deuda.totalPagado >= deuda.totalDeuda

            val progresoPct = if (deuda.totalDeuda > 0) {
                ((deuda.totalPagado / deuda.totalDeuda) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            if (esPagadaCompletamente) {
                tvLabelMontoTop.text = "Deuda Total"
                tvValorRestanteTop.text = formatMoneda.format(deuda.totalDeuda)
                tvBadgeEstado.text = "✓ PAGADA"
                tvBadgeEstado.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_low))
                tvBadgeEstado.setTextColor(ContextCompat.getColor(context, R.color.primary_emerald))
                btnAbonarCuota.visibility = View.GONE
                ivEditar.alpha = 0.3f
            } else {
                tvLabelMontoTop.text = "Pendiente"
                tvValorRestanteTop.text = formatMoneda.format(restante)
                tvBadgeEstado.text = if (progresoPct > 0) "AL DÍA" else "EN CURSO"
                tvBadgeEstado.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, if (progresoPct > 0) R.color.surface_container_low else R.color.debt_amber_bg))
                tvBadgeEstado.setTextColor(ContextCompat.getColor(context, if (progresoPct > 0) R.color.primary_emerald else R.color.debt_amber))
                btnAbonarCuota.visibility = View.VISIBLE
                ivEditar.alpha = 1.0f
            }

            tvPagadoLabel.text = "Pagado: ${formatMoneda.format(deuda.totalPagado)} ($progresoPct%)"
            tvRestanteLabel.text = "Restante: ${formatMoneda.format(restante)}"
            
            if (deuda.fechaInicio.isNotEmpty() || deuda.fechaFin.isNotEmpty()) {
                tvFechas.visibility = View.VISIBLE
                tvFechas.text = "Plazo: ${deuda.fechaInicio} - ${deuda.fechaFin}"
            } else {
                tvFechas.visibility = View.GONE
            }

            pbDeuda.max = 100
            pbDeuda.progress = progresoPct

            ivEditar.setOnClickListener {
                if (esPagadaCompletamente) {
                    Toast.makeText(context, "Las deudas completadas no se pueden editar", Toast.LENGTH_SHORT).show()
                } else {
                    onEditarClick(deuda)
                }
            }
            ivEliminar.setOnClickListener { onEliminarClick(deuda) }
            btnAbonarCuota.setOnClickListener {
                if (!esPagadaCompletamente) {
                    onAbonarClick(deuda)
                }
            }
        }
    }

    fun actualizarLista(nuevaLista: List<Deuda>) {
        submitList(nuevaLista)
    }
}