package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nexu.sockets.SendMessagePayload
import com.example.nexu.sockets.SocketEventBus
import com.example.nexu.sockets.SocketManager
import com.example.nexu.sockets.toJson
import com.example.nexu.sockets.toNewNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ChatActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var emailActual: String

    private lateinit var other_id: String

    private lateinit var chat_id: String
    private lateinit var current_user_id: String

    private var isFirst: Boolean = false
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


        // Datos del Chat: si es first chat, no encontrara chat_id pero si other_id, y viceversa si firt chat es false
        isFirst = intent.getBooleanExtra("ifFirst", false)

        chat_id  = intent.getStringExtra("chat_id") ?: ""
        other_id = intent.getStringExtra("other_id") ?: ""


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
        // Cargar Eventos de sockets
        observeSocketsEvents()
    }

    // RECEPCION DE EVENTOS DE SOCKETS
    private fun observeSocketsEvents(){
        Log.i("SOCKET", "Setteando listeners de sockets")
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                SocketEventBus.events.collect { event ->
                    Log.i("SOCKET", "Llego el siguiente evento: ${event.name}")
                    Log.i("SOCKET", "Con la siguiente data: ${event.data}")
                    event.toNewNotification()?.let { notificationPayload ->
                        Log.i("SOCKET", "Mensaje de: ${notificationPayload.sender_name}")
                        Log.i("SOCKET", "Mensaje: ${notificationPayload.message}")
                        Log.i("SOCKET", "Timestamp: ${notificationPayload.timestamp}")
                        val new_msg = Mensaje(
                            notificationPayload.message,
                            notificationPayload.sender_id,
                            "",
                            parseTimeStamp(notificationPayload.timestamp)
                        )
                        with(Dispatchers.Main){
                            listaMensajes.add(new_msg)
                            actualizarRecycler()
                        }
                    }

                }
            }
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
            autor = current_user_id,
            receptor = other_id,
            timestamp = System.currentTimeMillis()
        )

        listaMensajes.add(mensaje)
        if(isFirst){
            val payload = SendMessagePayload(
                target_id = other_id,    // ESTO ESTA MAL, TIENE QUE SER EL ID DEL OTRO USER
                content = texto
            ).toJson()
            SocketManager.emit("start_chat", payload)
        }else{
            val payload = SendMessagePayload(
                target_id = chat_id,
                content = texto
            ).toJson()

            SocketManager.emit("dm", payload)
        }

        edtMensaje.setText("")
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
                        data.forEach {
                            val msgUi = parseMessageApiToMensaje(it)
                            listaMensajes.add(msgUi)
                        }
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
    // ACTUALIZAR RECYCLER
    // ============================================================
    private fun actualizarRecycler() {
        recyclerMensajes.adapter = MensajeAdapter(listaMensajes, current_user_id)
        recyclerMensajes.scrollToPosition(listaMensajes.size - 1)
    }
}
