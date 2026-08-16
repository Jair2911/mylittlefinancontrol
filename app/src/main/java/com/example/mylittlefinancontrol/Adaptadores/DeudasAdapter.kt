package com.example.mylittlefinancontrol.Adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mylittlefinancontrol.Modelos.Deuda
import com.example.mylittlefinancontrol.databinding.ItemDeudaBinding
import java.text.NumberFormat
import java.util.Locale

class DeudasAdapter(
    private var listaDeudas: List<Deuda>,
    private val onEditarClick: (Deuda) -> Unit,
    private val onEliminarClick: (Deuda) -> Unit
) : RecyclerView.Adapter<DeudasAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDeudaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeudaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deuda = listaDeudas[position]
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        holder.binding.apply {
            tvConcepto.text = deuda.concepto
            tvTipo.text = deuda.tipo
            tvDescripcion.text = deuda.descripcion
            
            val restante = deuda.totalDeuda - deuda.totalPagado
            tvValorRestanteTop.text = format.format(restante)
            
            tvPagadoLabel.text = "Pagado: ${format.format(deuda.totalPagado)}"
            tvRestanteLabel.text = "Restante: ${format.format(restante)}"
            
            if (deuda.fechaInicio.isNotEmpty() || deuda.fechaFin.isNotEmpty()) {
                tvFechas.visibility = android.view.View.VISIBLE
                tvFechas.text = "Plazo: ${deuda.fechaInicio} - ${deuda.fechaFin}"
            } else {
                tvFechas.visibility = android.view.View.GONE
            }

            // Configurar ProgressBar
            pbDeuda.max = if (deuda.totalDeuda > 0) deuda.totalDeuda.toInt() else 100
            pbDeuda.progress = deuda.totalPagado.toInt()

            ivEditar.setOnClickListener { onEditarClick(deuda) }
            ivEliminar.setOnClickListener { onEliminarClick(deuda) }
        }
    }

    override fun getItemCount() = listaDeudas.size

    fun actualizarLista(nuevaLista: List<Deuda>) {
        listaDeudas = nuevaLista
        notifyDataSetChanged()
    }
}
