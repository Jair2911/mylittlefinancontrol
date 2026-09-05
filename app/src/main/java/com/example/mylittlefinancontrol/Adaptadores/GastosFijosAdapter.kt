package com.example.mylittlefinancontrol.Adaptadores

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        val context = holder.itemView.context

        holder.binding.apply {
            tvConcepto.text = gasto.concepto
            tvCategoriaTag.text = gasto.categoria.uppercase()
            tvMonto.text = format.format(gasto.monto)
            tvPagoInfo.text = "Día ${gasto.diaPago} · ${gasto.periodo}"

            // Icono y paleta cromática según categoría
            val catLower = gasto.categoria.lowercase()
            when {
                catLower.contains("serv") || catLower.contains("básic") || catLower.contains("luz") || catLower.contains("agua") || catLower.contains("gas") -> {
                    flIconoGf.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
                    ivIconoGf.setImageResource(R.drawable.ic_balance)
                    ivIconoGf.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_primary_fixed))
                }
                catLower.contains("viv") || catLower.contains("arriend") || catLower.contains("casa") || catLower.contains("hogar") -> {
                    flIconoGf.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                    ivIconoGf.setImageResource(R.drawable.ic_cottage)
                    ivIconoGf.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_secondary_container))
                }
                catLower.contains("tele") || catLower.contains("inter") || catLower.contains("fibra") || catLower.contains("movil") || catLower.contains("wifi") -> {
                    flIconoGf.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
                    ivIconoGf.setImageResource(R.drawable.ic_router)
                    ivIconoGf.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_primary_fixed))
                }
                catLower.contains("suscrip") || catLower.contains("stream") || catLower.contains("netf") || catLower.contains("spot") -> {
                    flIconoGf.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                    ivIconoGf.setImageResource(R.drawable.ic_subscriptions)
                    ivIconoGf.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_secondary_container))
                }
                catLower.contains("educ") || catLower.contains("univ") || catLower.contains("col") -> {
                    flIconoGf.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high))
                    ivIconoGf.setImageResource(R.drawable.ic_school)
                    ivIconoGf.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_variant))
                }
                else -> {
                    flIconoGf.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_low))
                    ivIconoGf.setImageResource(R.drawable.ic_more_horiz)
                    ivIconoGf.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }

            val cal = Calendar.getInstance()
            val mesAnioActual = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

            val bgPagado = ContextCompat.getColor(context, R.color.primary_fixed)
            val textPagado = ContextCompat.getColor(context, R.color.on_primary_fixed)
            val bgPendiente = ContextCompat.getColor(context, R.color.secondary_container)
            val textPendiente = ContextCompat.getColor(context, R.color.on_secondary_container)
            val bgPendienteMensual = ContextCompat.getColor(context, R.color.surface_container_high)
            val textPendienteMensual = ContextCompat.getColor(context, R.color.text_primary)

            if (gasto.periodo.contains("Quincenal", true)) {
                llQuincenas.visibility = View.VISIBLE
                tvEstadoMensual.visibility = View.GONE
                tvStatusLabel.text = "CUOTAS DEL MES:"

                // Q1
                if (gasto.ultimoPagoQ1 == mesAnioActual) {
                    tvEstadoQ1.text = "✓ Q1 PAGADA"
                    tvEstadoQ1.backgroundTintList = ColorStateList.valueOf(bgPagado)
                    tvEstadoQ1.setTextColor(textPagado)
                } else {
                    tvEstadoQ1.text = "⏱️ Q1 PENDIENTE"
                    tvEstadoQ1.backgroundTintList = ColorStateList.valueOf(bgPendiente)
                    tvEstadoQ1.setTextColor(textPendiente)
                }

                // Q2
                if (gasto.ultimoPagoQ2 == mesAnioActual) {
                    tvEstadoQ2.text = "✓ Q2 PAGADA"
                    tvEstadoQ2.backgroundTintList = ColorStateList.valueOf(bgPagado)
                    tvEstadoQ2.setTextColor(textPagado)
                } else {
                    tvEstadoQ2.text = "⏱️ Q2 PENDIENTE"
                    tvEstadoQ2.backgroundTintList = ColorStateList.valueOf(bgPendiente)
                    tvEstadoQ2.setTextColor(textPendiente)
                }
            } else {
                llQuincenas.visibility = View.GONE
                tvEstadoMensual.visibility = View.VISIBLE

                val estaPagado = gasto.ultimoPagoQ1 == mesAnioActual
                if (estaPagado) {
                    tvStatusLabel.text = "Abonado automáticamente"
                    tvEstadoMensual.text = "✓ PAGADO"
                    tvEstadoMensual.backgroundTintList = ColorStateList.valueOf(bgPagado)
                    tvEstadoMensual.setTextColor(textPagado)
                } else {
                    val dia = gasto.diaPago.trim()
                    if (dia == "30" || dia.contains("fin", true)) {
                        tvStatusLabel.text = "Vence a fin de mes"
                    } else {
                        tvStatusLabel.text = "Vence el día $dia"
                    }
                    tvEstadoMensual.text = "⏱️ PENDIENTE"
                    tvEstadoMensual.backgroundTintList = ColorStateList.valueOf(bgPendienteMensual)
                    tvEstadoMensual.setTextColor(textPendienteMensual)
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