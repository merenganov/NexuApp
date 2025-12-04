package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MessagesActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var emailActual: String
    private lateinit var rvChats: RecyclerView
    private lateinit var adapter: ChatAdapter

    private var listaChatsOriginal = listOf<ChatPreview>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)
        emailActual = sharedPref.getString("currentUser", "") ?: ""

        rvChats = findViewById(R.id.rvChats)
        rvChats.layoutManager = LinearLayoutManager(this)

        cargarListaChats()

        // ============= BUSCADOR =============
        val edtSearch = findViewById<android.widget.EditText>(R.id.edtSearch)
        edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarChats(s.toString())
            }
        })

        findViewById<LinearLayout>(R.id.navHomeM).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navProfileM).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun cargarListaChats() {

        val listaKey = "${emailActual}_chatlist"
        val correos = sharedPref.getStringSet(listaKey, emptySet())!!.toList()

        val listaChats = mutableListOf<ChatPreview>()

        correos.forEach { correoOtro ->

            val datos = sharedPref.getString(correoOtro, "")?.split("#") ?: listOf("Usuario")
            val nombre = datos.getOrNull(0) ?: correoOtro

            val chatInfoKey = "chatinfo_${emailActual}_$correoOtro"
            val rawChatInfo = sharedPref.getString(chatInfoKey, null)

            var ultimoMensaje = ""
            var timestamp = 0L

            if (rawChatInfo != null) {
                val parts = rawChatInfo.split("|")
                if (parts.size == 3) {
                    ultimoMensaje = parts[1]
                    timestamp = parts[2].toLongOrNull() ?: 0L
                }
            }

            listaChats.add(
                ChatPreview(
                    nombre = nombre,
                    correo = correoOtro,
                    ultimoMensaje = ultimoMensaje,
                    timestamp = timestamp
                )
            )
        }

        val ordenados = listaChats.sortedByDescending { it.timestamp }
        listaChatsOriginal = ordenados

        adapter = ChatAdapter(
            ordenados,
            onClick = { chat ->
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("emailOtro", chat.correo)
                intent.putExtra("nombreOtro", chat.nombre)
                startActivity(intent)
            },
            onLongClick = { chat ->
                eliminarConversacion(chat.correo)
                Toast.makeText(this, "Chat eliminado", Toast.LENGTH_SHORT).show()
                cargarListaChats()
            }
        )

        rvChats.adapter = adapter
    }
    private fun eliminarConversacion(emailOtro: String) {
        val key1 = "chat_${emailActual}_${emailOtro}"
        val key2 = "chat_${emailOtro}_${emailActual}"
        val listaKey = "${emailActual}_chatlist"
        val infoKey = "chatinfo_${emailActual}_$emailOtro"

        sharedPref.edit()
            .remove(key1)
            .remove(key2)
            .remove(infoKey)
            .apply()

        val set = sharedPref.getStringSet(listaKey, mutableSetOf())!!.toMutableSet()
        set.remove(emailOtro)
        sharedPref.edit().putStringSet(listaKey, set).apply()
    }


    // 🔥 FILTRO INTELIGENTE
    private fun filtrarChats(query: String) {
        if (query.isBlank()) {
            adapter.updateList(listaChatsOriginal)
            return
        }

        val filtrados = listaChatsOriginal.filter {
            it.nombre.lowercase().contains(query.lowercase())
        }

        adapter.updateList(filtrados)
    }
}

