package com.example.mylittlefinancontrol

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class NotasAdaptador(
    private var movimientos: List<Movimiento>,
    private val context: Context,
    private val onEditarClick: (Movimiento) -> Unit,
    private val onEliminarClick: (Movimiento) -> Unit
) : RecyclerView.Adapter<NotasAdaptador.NotaViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movimiento, parent, false)
        return NotaViewHolder(view)
    }

    override fun getItemCount(): Int {
        return movimientos.size
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val movimiento = movimientos[position]
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        holder.item_descripcion.text = "Concepto: " + movimiento.descripcion
        holder.item_valor.text = "Monto: ${format.format(movimiento.valor)}"
        holder.itemTipoReg.text = "Tipo: " + movimiento.tipo_reg

        holder.ivActualizar.setOnClickListener{
            onEditarClick(movimiento)
        }

        holder.ivEliminarMov.setOnClickListener {
            onEliminarClick(movimiento)
        }
    }

    class NotaViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val item_descripcion : TextView = itemView.findViewById(R.id.item_descripcion)
        val item_valor : TextView = itemView.findViewById(R.id.item_valor)
        val itemTipoReg : TextView = itemView.findViewById(R.id.item_tipo)

        val ivActualizar : ImageView = itemView.findViewById(R.id.ivActualizar)
        val ivEliminarMov : ImageView = itemView.findViewById(R.id.ivEliminarMov)
    }

    fun refrescarLista(nuevoMovimiento : List<Movimiento>){
        movimientos = nuevoMovimiento
        notifyDataSetChanged()
    }
}