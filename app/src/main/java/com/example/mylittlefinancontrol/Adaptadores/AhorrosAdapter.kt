package com.example.mylittlefinancontrol.Adaptadores

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mylittlefinancontrol.Modelos.Ahorro
import com.example.mylittlefinancontrol.R
import com.example.mylittlefinancontrol.databinding.ItemAhorroBinding
import java.text.NumberFormat
import java.util.*

class AhorrosAdapter(
    private var listaAhorros: List<Ahorro>,
    private val onEditarClick: (Ahorro) -> Unit,
    private val onEliminarClick: (Ahorro) -> Unit,
    private val onAbonarClick: (Ahorro) -> Unit
) : RecyclerView.Adapter<AhorrosAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAhorroBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAhorroBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ahorro = listaAhorros[position]
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val context = holder.itemView.context

        holder.binding.apply {
            tvTituloAhorro.text = ahorro.titulo
            tvCategoriaTagAhorro.text = ahorro.categoria.uppercase()
            tvMetaFecha.text = if (ahorro.fechaMeta.isNotEmpty()) "Meta: ${ahorro.fechaMeta}" else "Sin fecha límite"
            tvMontoActualAhorro.text = format.format(ahorro.montoActual)
            tvMontoObjetivoAhorro.text = "de ${format.format(ahorro.montoObjetivo)}"

            val porcentaje = if (ahorro.montoObjetivo > 0) {
                ((ahorro.montoActual / ahorro.montoObjetivo) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            tvPorcentajeAhorro.text = "$porcentaje%"
            pbProgresoAhorro.progress = porcentaje

            // Iconos y colores temáticos según categoría
            val catLower = ahorro.categoria.lowercase()
            when {
                catLower.contains("emerg") || catLower.contains("salud") || catLower.contains("segur") -> {
                    flIconoAhorro.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
                    ivIconoAhorro.setImageResource(R.drawable.ic_shield)
                    ivIconoAhorro.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_primary_fixed))
                }
                catLower.contains("veh") || catLower.contains("auto") || catLower.contains("carro") || catLower.contains("moto") -> {
                    flIconoAhorro.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                    ivIconoAhorro.setImageResource(R.drawable.ic_directions_car)
                    ivIconoAhorro.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_secondary_container))
                }
                catLower.contains("viaj") || catLower.contains("vaca") || catLower.contains("vuelo") -> {
                    flIconoAhorro.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high))
                    ivIconoAhorro.setImageResource(R.drawable.ic_cloud)
                    ivIconoAhorro.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_variant))
                }
                catLower.contains("viv") || catLower.contains("casa") || catLower.contains("hogar") || catLower.contains("depa") -> {
                    flIconoAhorro.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                    ivIconoAhorro.setImageResource(R.drawable.ic_cottage)
                    ivIconoAhorro.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_secondary_container))
                }
                catLower.contains("tec") || catLower.contains("pc") || catLower.contains("cel") || catLower.contains("comp") -> {
                    flIconoAhorro.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high))
                    ivIconoAhorro.setImageResource(R.drawable.ic_router)
                    ivIconoAhorro.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_variant))
                }
                else -> {
                    flIconoAhorro.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
                    ivIconoAhorro.setImageResource(R.drawable.ic_savings)
                    ivIconoAhorro.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_primary_fixed))
                }
            }

            btnEditarAhorro.setOnClickListener { onEditarClick(ahorro) }
            btnEliminarAhorro.setOnClickListener { onEliminarClick(ahorro) }
            btnAbonoRapido.setOnClickListener { onAbonarClick(ahorro) }
        }
    }

    override fun getItemCount() = listaAhorros.size

    fun actualizarLista(nuevaLista: List<Ahorro>) {
        listaAhorros = nuevaLista
        notifyDataSetChanged()
    }
}
