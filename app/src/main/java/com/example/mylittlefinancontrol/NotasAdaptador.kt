package com.example.mylittlefinancontrol

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

sealed class MovimientoListItem {
    data class Header(val fechaTitulo: String, val conteoTexto: String) : MovimientoListItem()
    data class Item(val movimiento: Movimiento) : MovimientoListItem()
}

class NotasAdaptador(
    private var listaOriginal: List<Movimiento>,
    private val context: Context,
    private val onEditarClick: (Movimiento) -> Unit,
    private val onEliminarClick: (Movimiento) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private val displayItems = mutableListOf<MovimientoListItem>()

    init {
        procesarGrupos(listaOriginal)
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is MovimientoListItem.Header -> TYPE_HEADER
            is MovimientoListItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movimiento_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movimiento, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun getItemCount(): Int = displayItems.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is MovimientoListItem.Header -> {
                val headerHolder = holder as HeaderViewHolder
                headerHolder.tvFecha.text = item.fechaTitulo
                headerHolder.tvConteo.text = item.conteoTexto
            }
            is MovimientoListItem.Item -> {
                val itemHolder = holder as ItemViewHolder
                val movimiento = item.movimiento
                val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

                itemHolder.itemDescripcion.text = movimiento.descripcion

                // Parsear hora de la fecha si existe
                val horaStr = obtenerHoraFormateada(movimiento)
                itemHolder.itemFecha.text = horaStr

                val descUpper = movimiento.descripcion.uppercase()
                val tipoUpper = movimiento.tipo_reg.uppercase()

                // Determinar icono, badges, métodos de pago y colores
                when {
                    tipoUpper.contains("GASTO FIJO") || descUpper.contains("GASTO FIJO") || descUpper.startsWith("PAGO:") -> {
                        itemHolder.ivIconoMovimiento.setImageResource(R.drawable.ic_autorenew)
                        itemHolder.ivIconoMovimiento.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_emerald))
                        itemHolder.flIconoContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
                        itemHolder.itemTipoReg.text = "GASTO FIJO"
                        itemHolder.itemTipoReg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                        itemHolder.itemTipoReg.setTextColor(ContextCompat.getColor(context, R.color.on_secondary_container))
                        itemHolder.tvMetodoPagoSub.text = "Débito aut."
                    }
                    descUpper.contains("SUPERMERCADO") || descUpper.contains("DESPENSA") || descUpper.contains("LÍDER") || descUpper.contains("ALIMENTOS") -> {
                        itemHolder.ivIconoMovimiento.setImageResource(R.drawable.ic_shopping_cart)
                        itemHolder.ivIconoMovimiento.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
                        itemHolder.flIconoContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high))
                        itemHolder.itemTipoReg.text = "Alimentos"
                        itemHolder.itemTipoReg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container))
                        itemHolder.itemTipoReg.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        itemHolder.tvMetodoPagoSub.text = "Tarjeta Visa"
                    }
                    tipoUpper.contains("INGRESO") || descUpper.contains("HONORARIOS") || descUpper.contains("SUELDO") || descUpper.contains("TRANSFERENCIA") -> {
                        itemHolder.ivIconoMovimiento.setImageResource(R.drawable.ic_payments)
                        itemHolder.ivIconoMovimiento.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_emerald))
                        itemHolder.flIconoContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_fixed))
                        itemHolder.itemTipoReg.text = if (descUpper.contains("HONORARIOS")) "Honorarios" else "Ingreso"
                        itemHolder.itemTipoReg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container))
                        itemHolder.itemTipoReg.setTextColor(ContextCompat.getColor(context, R.color.primary_emerald))
                        itemHolder.tvMetodoPagoSub.text = "Banco Estado"
                    }
                    descUpper.contains("INTERNET") || descUpper.contains("FIBRA") || descUpper.contains("LUZ") || descUpper.contains("AGUA") || descUpper.contains("SERVICIOS") -> {
                        itemHolder.ivIconoMovimiento.setImageResource(R.drawable.ic_wifi)
                        itemHolder.ivIconoMovimiento.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
                        itemHolder.flIconoContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_container))
                        itemHolder.itemTipoReg.text = "Servicios"
                        itemHolder.itemTipoReg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container))
                        itemHolder.itemTipoReg.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        itemHolder.tvMetodoPagoSub.text = "Gasto Fijo"
                    }
                    else -> {
                        itemHolder.ivIconoMovimiento.setImageResource(R.drawable.ic_receipt_long)
                        itemHolder.ivIconoMovimiento.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
                        itemHolder.flIconoContainer.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_low))
                        itemHolder.itemTipoReg.text = movimiento.tipo_reg.uppercase()
                        itemHolder.itemTipoReg.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container))
                        itemHolder.itemTipoReg.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        itemHolder.tvMetodoPagoSub.text = "Débito / Efectivo"
                    }
                }

                if (tipoUpper.contains("INGRESO")) {
                    itemHolder.itemValor.text = "+${format.format(movimiento.valor)}"
                    itemHolder.itemValor.setTextColor(ContextCompat.getColor(context, R.color.primary_emerald))
                } else {
                    itemHolder.itemValor.text = "-${format.format(movimiento.valor)}"
                    itemHolder.itemValor.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                }

                itemHolder.ivActualizar.setOnClickListener {
                    onEditarClick(movimiento)
                }

                itemHolder.ivEliminarMov.setOnClickListener {
                    onEliminarClick(movimiento)
                }
            }
        }
    }

    private fun normalizarFechaClave(fechaRaw: String): String {
        if (fechaRaw.isBlank()) return "Sin Fecha"
        val timestamp = fechaRaw.trim().toLongOrNull()
        if (timestamp != null && timestamp > 100000000000L) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date(timestamp))
        }
        val clean = fechaRaw.trim().split(" ")[0]
        val delimiters = charArrayOf('-', '/', '.')
        for (d in delimiters) {
            if (clean.contains(d)) {
                val parts = clean.split(d)
                if (parts.size >= 3) {
                    val p0 = parts[0].toIntOrNull()
                    val p1 = parts[1].toIntOrNull()
                    val p2 = parts[2].toIntOrNull()
                    if (p0 != null && p1 != null && p2 != null) {
                        if (p0 > 1000) {
                            return String.format(Locale.US, "%04d-%02d-%02d", p0, p1, p2)
                        } else if (p2 > 1000) {
                            return String.format(Locale.US, "%04d-%02d-%02d", p2, p1, p0)
                        }
                    }
                }
            }
        }
        return clean
    }

    private fun obtenerHoraFormateada(movimiento: Movimiento): String {
        val fechaCompleta = if (movimiento.fecha.isNotBlank()) movimiento.fecha else movimiento.fechaCreacion
        return try {
            val ts = fechaCompleta.trim().toLongOrNull()
            if (ts != null && ts > 100000000000L) {
                val sdf = SimpleDateFormat("HH:mm 'hrs'", Locale("es", "ES"))
                return sdf.format(Date(ts))
            }
            if (fechaCompleta.contains(" ")) {
                val partes = fechaCompleta.split(" ")
                val horaPartes = partes[1].split(":")
                "${horaPartes[0]}:${horaPartes[1]} hrs"
            } else {
                "12:00 hrs"
            }
        } catch (e: Exception) {
            "12:00 hrs"
        }
    }

    private fun procesarGrupos(movimientos: List<Movimiento>) {
        displayItems.clear()
        if (movimientos.isEmpty()) {
            notifyDataSetChanged()
            return
        }

        // Ordenar dando prioridad a la Fecha de Movimiento, fallback a Fecha de Creación
        val movimientosOrdenados = movimientos.sortedWith(
            compareByDescending<Movimiento> { NotasDatabaseHelper.obtenerTimestampParaOrdenamiento(it) }
                .thenByDescending { it.id }
        )

        val sdfParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calHoy = Calendar.getInstance()
        val strHoy = sdfParser.format(calHoy.time)

        val calAyer = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val strAyer = sdfParser.format(calAyer.time)

        val sdfHeader = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

        // Agrupar por clave de fecha normalizada 'yyyy-MM-dd'
        val agrupados = LinkedHashMap<String, MutableList<Movimiento>>()
        for (mov in movimientosOrdenados) {
            val fechaEfectiva = if (mov.fecha.isNotBlank()) mov.fecha else mov.fechaCreacion
            val keyFecha = normalizarFechaClave(fechaEfectiva)
            agrupados.getOrPut(keyFecha) { mutableListOf() }.add(mov)
        }

        for ((fechaKey, lista) in agrupados) {
            val tituloHeader = when (fechaKey) {
                strHoy -> {
                    try {
                        val d = sdfParser.parse(fechaKey)
                        "HOY • ${sdfHeader.format(d ?: Date()).uppercase()}"
                    } catch (e: Exception) { "HOY" }
                }
                strAyer -> {
                    try {
                        val d = sdfParser.parse(fechaKey)
                        "AYER • ${sdfHeader.format(d ?: Date()).uppercase()}"
                    } catch (e: Exception) { "AYER" }
                }
                else -> {
                    try {
                        val d = sdfParser.parse(fechaKey)
                        sdfHeader.format(d ?: Date()).uppercase()
                    } catch (e: Exception) { fechaKey }
                }
            }

            val conteoTexto = if (lista.size == 1) "1 Movimiento" else "${lista.size} Movimientos"
            displayItems.add(MovimientoListItem.Header(tituloHeader, conteoTexto))

            for (mov in lista) {
                displayItems.add(MovimientoListItem.Item(mov))
            }
        }
    }

    fun refrescarLista(nuevosMovimientos: List<Movimiento>) {
        listaOriginal = nuevosMovimientos
        procesarGrupos(listaOriginal)
        notifyDataSetChanged()
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFecha: TextView = itemView.findViewById(R.id.tv_header_fecha)
        val tvConteo: TextView = itemView.findViewById(R.id.tv_header_conteo)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flIconoContainer: FrameLayout = itemView.findViewById(R.id.fl_icono_container)
        val ivIconoMovimiento: ImageView = itemView.findViewById(R.id.iv_icono_movimiento)
        val itemDescripcion: TextView = itemView.findViewById(R.id.item_descripcion)
        val itemValor: TextView = itemView.findViewById(R.id.item_valor)
        val itemTipoReg: TextView = itemView.findViewById(R.id.item_tipo)
        val itemFecha: TextView = itemView.findViewById(R.id.item_fecha)
        val tvMetodoPagoSub: TextView = itemView.findViewById(R.id.tv_metodo_pago_sub)
        val ivActualizar: View = itemView.findViewById(R.id.ivActualizar)
        val ivEliminarMov: View = itemView.findViewById(R.id.ivEliminarMov)
    }
}