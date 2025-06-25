package com.example.mylittlefinancontrol

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class NotasAdaptador(
    private var movimientos: List<Movimiento>,
    context: Context) : RecyclerView.Adapter<NotasAdaptador.NotaViewHolder>()
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

        holder.item_descripcion.text = movimiento.descripcion
        holder.item_valor.text = movimiento.valor.toString()
        holder.itemTipoReg.text = movimiento.tipo_reg

        holder.ivActualizar.setOnClickListener{
            val intent = Intent(holder.itemView.context, ActualizarMovimientoActivity::class.java).apply {
                putExtra("id_movimiento", movimiento.id)
            }

            holder.itemView.context.startActivity(intent)
        }

        holder.ivEliminarMov.setOnClickListener {
            val intent = Intent(holder.itemView.context, EliminarMovimientoActivity::class.java).apply {
                putExtra("id_movimiento", movimiento.id)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    class NotaViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val item_descripcion : TextView = itemView.findViewById(R.id.item_descripcion)
        val item_valor : TextView = itemView.findViewById(R.id.item_valor)
        val itemTipoReg : TextView = itemView.findViewById(R.id.item_tipo)
        val itemFecha = Constantes.formatoFecha(Constantes.obtenerTiempoD());

        val ivActualizar : ImageView = itemView.findViewById(R.id.ivActualizar)
        val ivEliminarMov : ImageView = itemView.findViewById(R.id.ivEliminarMov)
    }

    fun refrescarLista(nuevoMovimiento : List<Movimiento>){
        movimientos = nuevoMovimiento
        notifyDataSetChanged()
    }
}