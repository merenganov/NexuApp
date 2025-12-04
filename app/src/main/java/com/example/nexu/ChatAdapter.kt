package com.example.nexu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private var lista: List<ChatPreview>,
    private val onClick: (ChatPreview) -> Unit,
    private val onLongClick: (ChatPreview) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.txtNombreChatItem)
        val txtMensaje: TextView = view.findViewById(R.id.txtUltimoMensaje)

        fun bind(chat: ChatPreview) {
            txtNombre.text = chat.nombre

            txtMensaje.text =
                if (chat.ultimoMensaje.isBlank()) "Sin mensajes"
                else chat.ultimoMensaje

            itemView.setOnClickListener { onClick(chat) }

            itemView.setOnLongClickListener {
                onLongClick(chat)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    // ======================================================
    //  Método para actualizar la lista filtrada
    // ======================================================
    fun updateList(newList: List<ChatPreview>) {
        lista = newList
        notifyDataSetChanged()
    }
}

