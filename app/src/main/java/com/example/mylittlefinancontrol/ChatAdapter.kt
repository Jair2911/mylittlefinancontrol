package com.example.mylittlefinancontrol

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class MensajeChat(val texto: String, val esUsuario: Boolean)

class ChatAdapter(private val mensajes: List<MensajeChat>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_mensaje, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val mensaje = mensajes[position]
        holder.tvMensaje.text = mensaje.texto
        
        val params = holder.tvMensaje.layoutParams as LinearLayout.LayoutParams
        if (mensaje.esUsuario) {
            params.gravity = Gravity.END
            holder.tvMensaje.setBackgroundResource(R.drawable.bg_user_message)
            holder.tvMensaje.setTextColor(android.graphics.Color.WHITE)
        } else {
            params.gravity = Gravity.START
            holder.tvMensaje.setBackgroundResource(R.drawable.bg_bot_message)
            holder.tvMensaje.setTextColor(android.graphics.Color.BLACK)
        }
        holder.tvMensaje.layoutParams = params
    }

    override fun getItemCount() = mensajes.size
}
