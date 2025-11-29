package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        // Inicializar SharedPreferences
        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)

        // Obtener usuario
        val email = sharedPref.getString("currentUser", null) ?: ""

        // Obtener datos del usuario
        val data = sharedPref.getString(email, null)
        val username = data?.split("#")?.getOrNull(0) ?: "Usuario"

        // Mostrar bienvenida
        val welcomeText: TextView = findViewById(R.id.welcomeText)
        welcomeText.text = "Bienvenid@ $username"

        // Ir al perfil
        findViewById<LinearLayout>(R.id.profileSection).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        // Ir a mensajes
        findViewById<LinearLayout>(R.id.messagesection).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
            finish()
        }

        // RecyclerView del feed
        val rvFeed = findViewById<RecyclerView>(R.id.rvFeed)
        rvFeed.layoutManager = LinearLayoutManager(this)

        // Cargar publicaciones
        rvFeed.adapter = PostAdapter(
            context = this,
            lista = obtenerPublicacionesGlobales(),
            onEdit = { /* No se edita en Home */ },
            onDelete = { /* No se elimina en Home */ }
        )

    }

    private fun obtenerPublicacionesGlobales(): List<Post> {

        val lista = mutableListOf<Post>()

        for (key in sharedPref.all.keys) {

            if (key.endsWith("_posts")) {

                val rawPosts = sharedPref.getStringSet(key, emptySet()) ?: emptySet()

                for (p in rawPosts) {
                    val parts = p.split("|")
                    if (parts.size == 4) {
                        lista.add(
                            Post(
                                nombre = parts[0],
                                carrera = parts[1],
                                tag = parts[2],
                                contenido = parts[3]
                            )
                        )
                    }
                }
            }
        }

        // Opcional: ordenar (primero las más nuevas)
        return lista.sortedByDescending { it.contenido.length }
    }
}
