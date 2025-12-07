package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
        val jwt = sharedPref.getString("token", "") ?: ""


        rvChats = findViewById(R.id.rvChats)
        rvChats.layoutManager = LinearLayoutManager(this)

        cargarChats(jwt)

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


    private fun cargarChats(jwt: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                RetrofitClient.api.getChats("Bearer $jwt")
            }

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { response ->
                        if (!response.isSuccessful) {
                            mostrarError("Error al mandar la petición")
                            return@fold
                        }

                        val data = response.body()?.data
                        if (data == null) {
                            mostrarError("Error al procesar los datos")
                            return@fold
                        }

                        // Convertimos la lista
                        val listaChats = data.map { chatSummary ->
                            ChatSummaryToChatPreview(chatSummary)
                        }.sortedByDescending { it.timestamp }

                        listaChatsOriginal = listaChats

                        configurarAdapter(listaChats)
                    },

                    onFailure = {
                        mostrarError("Error de conexión")
                    }
                )
            }
        }
    }

    private fun ChatSummaryToChatPreview(c: ChatSummary): ChatPreview{
        return ChatPreview(
            nombre = c.otherUser.name,
            id = c.id,
            ultimoMensaje = c.lastMessage?.content.orEmpty(),
            timestamp = parseTimeStamp(c.lastMessage?.timestamp)
        )
    }

    // TODO: no se maneja timezone en ningun lado de la app
    private fun parseTimeStamp(timestamp: String?): Long {
        if (timestamp == null) return  0L
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        return sdf.parse(timestamp)?.time ?: 0L
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun configurarAdapter(lista: List<ChatPreview>) {
        adapter = ChatAdapter(
            lista,
            onClick = { chat ->
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("chat_id", chat.id)
                intent.putExtra("nombreOtro", chat.nombre)
                startActivity(intent)
            },
        )

        rvChats.adapter = adapter
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

