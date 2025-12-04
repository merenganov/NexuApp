package com.example.nexu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(
    private val context: Context,
    private val lista: List<Post>,
    private val onDelete: (Post) -> Unit,
    private val onItemClick: (Post) -> Unit   // <--- NUEVO callback
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtNombre: TextView = view.findViewById(R.id.txtItemNombre)
        val txtCarrera: TextView = view.findViewById(R.id.txtItemCarrera)
        val txtTag: TextView = view.findViewById(R.id.txtItemTag)
        val txtContenido: TextView = view.findViewById(R.id.txtItemContenido)
        val btnMenu: ImageView = view.findViewById(R.id.btnItemMenu)

        // Este es el layout raiz que hiciste clickeable en item_post.xml
        private val cardPost: View = view.findViewById(R.id.cardPost)

        fun bind(post: Post) {

            txtNombre.text = post.nombre
            txtCarrera.text = post.carrera
            txtTag.text = "#${post.tag}"
            txtContenido.text = post.contenido

            // -----------------------------
            //   CLICK EN LA PUBLICACIÓN
            // -----------------------------
            cardPost.setOnClickListener {
                onItemClick(post)
            }

            // -----------------------------
            //     MENÚ "ELIMINAR"
            // -----------------------------
            btnMenu.setOnClickListener {
                val popup = PopupMenu(context, btnMenu)
                popup.menuInflater.inflate(R.menu.menu_post_item, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.opEliminarPost -> onDelete(post)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }
}
