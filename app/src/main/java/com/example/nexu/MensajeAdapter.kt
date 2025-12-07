package com.example.nexu

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.log

class MensajeAdapter(
    private val lista: List<Mensaje>,
    private val user_id_actual: String
) : RecyclerView.Adapter<MensajeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMensaje: TextView = view.findViewById(R.id.txtMensaje)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layout = if (viewType == 1)
            R.layout.item_mensaje_propio
        else
            R.layout.item_mensaje_otro

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        val autor = lista[position].autor
        Log.i("CHAT_DEBUG", "User_id: $user_id_actual")
        Log.i("CHAT_DEBUG", "Autor: $autor")
        return if (autor == user_id_actual) 1 else 0
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.txtMensaje.text = lista[position].texto
    }
}
