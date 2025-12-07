import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nexu.ChatPreview
import com.example.nexu.R
import com.example.nexu.RetrofitClient


class ChatAdapter(
    private var lista: List<ChatPreview>,
    private val onClick: (ChatPreview) -> Unit,
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.txtNombreChatItem)
        val txtMensaje: TextView = view.findViewById(R.id.txtUltimoMensaje)
        val imgAvatar: ImageView = view.findViewById(R.id.imgAvatarChat)

        fun bind(chat: ChatPreview) {
            txtNombre.text = chat.nombre

            txtMensaje.text =
                if (chat.ultimoMensaje.isBlank()) "Sin mensajes"
                else chat.ultimoMensaje

            // ------------------------
            // Cargar foto de perfil
            // ------------------------
            val url = chat.fotoPerfilUrl

            Glide.with(itemView.context)
                .load(url)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgAvatar)

            itemView.setOnClickListener { onClick(chat) }
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

    fun updateList(newList: List<ChatPreview>) {
        lista = newList
        notifyDataSetChanged()
    }
}
