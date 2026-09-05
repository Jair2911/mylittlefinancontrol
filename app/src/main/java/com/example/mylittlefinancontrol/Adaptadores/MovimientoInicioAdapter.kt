package com.example.mylittlefinancontrol.Adaptadores

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mylittlefinancontrol.Movimiento
import com.example.mylittlefinancontrol.R
import java.text.NumberFormat
import java.util.Locale

class MovimientoInicioAdapter(private var lista: List<Movimiento>) : 
    RecyclerView.Adapter<MovimientoInicioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val flIconoContenedor: FrameLayout = view.findViewById(R.id.fl_icono_contenedor)
        val icono: ImageView = view.findViewById(R.id.iv_tipo_icono)
        val descripcion: TextView = view.findViewById(R.id.tv_descripcion)
        val categoriaBadge: TextView = view.findViewById(R.id.tv_categoria_badge)
        val fecha: TextView = view.findViewById(R.id.tv_fecha)
        val monto: TextView = view.findViewById(R.id.tv_monto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento_inicio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mov = lista[position]
        val context = holder.itemView.context
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        holder.descripcion.text = mov.descripcion
        holder.fecha.text = mov.fecha

        val esIngreso = mov.tipo_reg.contains("INGRESO", true)
        val descLower = mov.descripcion.lowercase()

        val montoFormateado = format.format(mov.valor)
        if (esIngreso) {
            holder.monto.text = "+$montoFormateado"
            holder.monto.setTextColor(ContextCompat.getColor(context, R.color.primary_emerald))
            holder.categoriaBadge.text = "Ingreso"
            holder.categoriaBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
            holder.categoriaBadge.setTextColor(ContextCompat.getColor(context, R.color.on_primary_fixed))
            
            holder.flIconoContenedor.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
            holder.icono.setImageResource(R.drawable.ic_add)
            holder.icono.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_primary_fixed))
        } else {
            holder.monto.text = "-$montoFormateado"
            holder.monto.setTextColor(ContextCompat.getColor(context, R.color.text_primary))

            when {
                mov.gastoFijoId != null || descLower.contains("pago") || descLower.contains("fijo") -> {
                    holder.categoriaBadge.text = "Fijo"
                    holder.categoriaBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high))
                    holder.categoriaBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

                    holder.flIconoContenedor.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high))
                    holder.icono.setImageResource(R.drawable.ic_balance)
                    holder.icono.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
                }
                descLower.contains("super") || descLower.contains("mercado") || descLower.contains("compra") -> {
                    holder.categoriaBadge.text = "Supermercado"
                    holder.categoriaBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
                    holder.categoriaBadge.setTextColor(ContextCompat.getColor(context, R.color.on_primary_fixed))

                    holder.flIconoContenedor.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
                    holder.icono.setImageResource(R.drawable.ic_shopping_cart)
                    holder.icono.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_primary_fixed))
                }
                else -> {
                    holder.categoriaBadge.text = "Varios"
                    holder.categoriaBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                    holder.categoriaBadge.setTextColor(ContextCompat.getColor(context, R.color.on_secondary_container))

                    holder.flIconoContenedor.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                    holder.icono.setImageResource(R.drawable.ic_shopping_bag)
                    holder.icono.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.on_secondary_container))
                }
            }
        }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<Movimiento>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
