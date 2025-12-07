package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
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

class ChatActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var emailActual: String
//    TODO: quitar emailOtro, no se va a usar
    private lateinit var emailOtro: String

    private lateinit var chat_id: String
    private lateinit var current_user_id: String

    private lateinit var recyclerMensajes: RecyclerView
    private var listaMensajes = mutableListOf<Mensaje>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        // Preferencias
        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)

        // Usuario actual
        emailActual = sharedPref.getString("currentUser", "") ?: ""

        current_user_id = sharedPref.getString("currentUserId", "") ?: ""

        val jwt = sharedPref.getString("token", "") ?: ""

        // Datos del Chat
        chat_id  = intent.getStringExtra("chat_id") ?: ""
        emailOtro = intent.getStringExtra("emailOtro") ?: ""    // ESTO ES DONDE VA A ROMPER RN
        val nombreOtro = intent.getStringExtra("nombreOtro") ?: ""

        // Mostrar nombre arriba
        findViewById<TextView>(R.id.txtNombreChat).text = nombreOtro

        // Configurar RecyclerView
        recyclerMensajes = findViewById(R.id.recyclerMensajes)
        recyclerMensajes.layoutManager = LinearLayoutManager(this)

        // Cargar mensajes previos si los hay
        cargarMensajes(jwt, chat_id)

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

    private fun cargarMensajes(jwt:String, chat_id: String){
        lifecycleScope.launch(Dispatchers.IO){
            val result = runCatching {
                RetrofitClient.api.getMessages("Bearer $jwt",chat_id)
            }

            withContext(Dispatchers.Main){
                result.fold(
                    onSuccess =  { response ->
                        if (!response.isSuccessful){
                            mostrarError("Error al mandar la peticion")
                            return@fold
                        }

                        val data = response.body()?.data
                        if (data == null){
                            mostrarError("Error al procesar los datos")
                            return@fold
                        }

                        // Data es una List<Message>
                        val mensajes = data.map{ messageApi ->
                            parseMessageApiToMensaje(messageApi)
                        }.sortedByDescending { it.timestamp }
                        listaMensajes = mensajes as MutableList<Mensaje>
                        actualizarRecycler()
                    },
                    onFailure = {
                        mostrarError("Error de conexion")
                    }
                )
            }
        }
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun parseMessageApiToMensaje(msg: MessageApi): Mensaje{
        return Mensaje(
            texto = msg.content,
            autor = msg.senderId,
            receptor = "",
            timestamp = parseTimeStamp(msg.timestamp)
        )
    }

    // TODO: no se maneja timezone en ningun lado de la app
    private fun parseTimeStamp(timestamp: String?): Long {
        if (timestamp == null) return  0L
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        return sdf.parse(timestamp)?.time ?: 0L
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
        recyclerMensajes.adapter = MensajeAdapter(listaMensajes, current_user_id)
        recyclerMensajes.scrollToPosition(listaMensajes.size - 1)
    }
}
