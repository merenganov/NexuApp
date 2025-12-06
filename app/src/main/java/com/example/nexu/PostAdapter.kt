package com.example.nexu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class PostAdapter(
    private val context: Context,
    private val lista: MutableList<Post>,
    private val onDelete: (Post) -> Unit,
    private val onItemClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        val txtNombre: TextView = view.findViewById(R.id.txtItemNombre)
        val txtCarrera: TextView = view.findViewById(R.id.txtItemCarrera)
        val txtTag: TextView = view.findViewById(R.id.txtItemTag)
        val txtContenido: TextView = view.findViewById(R.id.txtItemContenido)
        val btnMenu: ImageView = view.findViewById(R.id.btnItemMenu)
        val cardPost: View = view.findViewById(R.id.cardPost)

        fun bind(post: Post) {

            txtNombre.text = post.user.name          // <-- CORRECTO
            txtCarrera.text = post.user.career       // <-- CORRECTO
            txtTag.text = "#${post.tag.name}"        // <-- CORRECTO
            txtContenido.text = post.description     // <-- CORRECTO

            Glide.with(context)
                .load(post.user.avatar_url)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgAvatar)

            cardPost.setOnClickListener {
                onItemClick(post)
            }

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

    // --------------------------------------------------
    // MÉTODOS PARA ACTUALIZAR LAS PUBLICACIONES
    // --------------------------------------------------

    fun setPosts(newList: List<Post>) {
        lista.clear()
        lista.addAll(newList)
        notifyDataSetChanged()
    }

    fun addPost(post: Post) {
        lista.add(0, post)
        notifyItemInserted(0)
    }

    fun removePostById(postId: String) {
        val index = lista.indexOfFirst { it.id == postId }
        if (index != -1) {
            lista.removeAt(index)
            notifyItemRemoved(index)
        }
    }

}
