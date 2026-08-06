package com.example.mylittlefinancontrol.Adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val descripcion: TextView = view.findViewById(R.id.tv_descripcion)
        val fecha: TextView = view.findViewById(R.id.tv_fecha)
        val monto: TextView = view.findViewById(R.id.tv_monto)
        val icono: ImageView = view.findViewById(R.id.iv_tipo_icono)
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
        holder.monto.text = format.format(mov.valor)

        if (mov.tipo_reg.contains("INGRESO", true)) {
            holder.monto.setTextColor(ContextCompat.getColor(context, R.color.verde_oscuro))
            holder.icono.setImageResource(R.drawable.ic_add)
        } else {
            holder.monto.setTextColor(ContextCompat.getColor(context, R.color.rojo_alerta))
            holder.icono.setImageResource(R.drawable.ic_movimientos)
        }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<Movimiento>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
