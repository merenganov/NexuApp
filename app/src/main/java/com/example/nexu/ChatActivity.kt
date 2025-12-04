package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var emailActual: String
    private lateinit var emailOtro: String

    private lateinit var recyclerMensajes: RecyclerView
    private val listaMensajes = mutableListOf<Mensaje>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        // Preferencias
        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)

        // Usuario actual
        emailActual = sharedPref.getString("currentUser", "") ?: ""

        // Datos del usuario con quien hablamos
        emailOtro = intent.getStringExtra("emailOtro") ?: ""
        val nombreOtro = intent.getStringExtra("nombreOtro") ?: ""

        // Mostrar nombre arriba
        findViewById<TextView>(R.id.txtNombreChat).text = nombreOtro

        // Configurar RecyclerView
        recyclerMensajes = findViewById(R.id.recyclerMensajes)
        recyclerMensajes.layoutManager = LinearLayoutManager(this)

        // Cargar mensajes previos si los hay
        cargarMensajes()

        // Botón enviar
        findViewById<ImageButton>(R.id.btnEnviar).setOnClickListener {
            enviarMensaje()
        }
        findViewById<ImageButton>(R.id.btnBackChat).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
            finish()
        }

    }

    // ============================================================
    // ENVIAR MENSAJE
    // ============================================================
    private fun enviarMensaje() {
        val edtMensaje = findViewById<EditText>(R.id.edtMensaje)
        val texto = edtMensaje.text.toString().trim()

        if (texto.isEmpty()) return

        val mensaje = Mensaje(
            texto = texto,
            autor = emailActual,
            receptor = emailOtro,
            timestamp = System.currentTimeMillis()
        )

        listaMensajes.add(mensaje)
        guardarMensajes()
        guardarChatEnLista(emailOtro, texto)


        edtMensaje.setText("")
        actualizarRecycler()
    }
    private fun guardarChatEnLista(emailOtro: String, ultimoMensaje: String) {
        val key = "${emailActual}_chatlist"

        // Recuperar la lista existente
        val lista = sharedPref.getStringSet(key, mutableSetOf())!!.toMutableSet()

        // Guardar email en la lista
        lista.add(emailOtro)
        sharedPref.edit().putStringSet(key, lista).apply()

        // Guardar también último mensaje y timestamp del chat
        val chatInfoKey = "chatinfo_${emailActual}_$emailOtro"
        val timestamp = System.currentTimeMillis()

        val nombreOtro = sharedPref.getString(emailOtro, "")?.split("#")?.getOrNull(0) ?: emailOtro

        val chatData = "$nombreOtro|$ultimoMensaje|$timestamp"

        sharedPref.edit().putString(chatInfoKey, chatData).apply()
    }


    // ============================================================
    // CARGAR HISTORIAL DEL CHAT
    // ============================================================
    private fun cargarMensajes() {
        val key1 = "chat_${emailActual}_${emailOtro}"
        val key2 = "chat_${emailOtro}_${emailActual}"

        // Busca mensajes en cualquiera de las dos combinaciones
        val raw = sharedPref.getStringSet(key1, null)
            ?: sharedPref.getStringSet(key2, emptySet())!!

        raw.forEach {
            val parts = it.split("|")
            if (parts.size == 4) {
                listaMensajes.add(
                    Mensaje(
                        texto = parts[0],
                        autor = parts[1],
                        receptor = parts[2],
                        timestamp = parts[3].toLong()
                    )
                )
            }
        }

        listaMensajes.sortBy { it.timestamp }
        actualizarRecycler()
    }

    // ============================================================
    // GUARDAR MENSAJES
    // ============================================================
    private fun guardarMensajes() {
        val key = "chat_${emailActual}_${emailOtro}"

        val set = listaMensajes.map {
            "${it.texto}|${it.autor}|${it.receptor}|${it.timestamp}"
        }.toSet()

        sharedPref.edit().putStringSet(key, set).apply()
    }

    // ============================================================
    // ACTUALIZAR RECYCLER
    // ============================================================
    private fun actualizarRecycler() {
        recyclerMensajes.adapter = MensajeAdapter(listaMensajes, emailActual)
        recyclerMensajes.scrollToPosition(listaMensajes.size - 1)
    }
}
