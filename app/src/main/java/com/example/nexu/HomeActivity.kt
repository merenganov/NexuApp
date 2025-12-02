package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var rvFeed: RecyclerView
    private lateinit var currentEmail: String
    private var listaGlobal: MutableList<Post> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)
        currentEmail = sharedPref.getString("currentUser", null) ?: ""

        val data = sharedPref.getString(currentEmail, "")!!.split("#")
        val username = data.getOrNull(0) ?: "Usuario"

        findViewById<TextView>(R.id.welcomeText).text = "Bienvenid@ $username"

        // Navegación

        findViewById<LinearLayout>(R.id.messagesection).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.profileSection).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)); finish()
        }

        // PUBLICAR DESDE HOME
        findViewById<EditText>(R.id.newPostInput).setOnClickListener {
            abrirDialogNuevaPublicacionHome()
        }

        // BUSCADOR
        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarBuscador(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // RECYCLER VIEW
        rvFeed = findViewById(R.id.rvFeed)
        rvFeed.layoutManager = LinearLayoutManager(this)

        listaGlobal = obtenerPublicacionesGlobales().toMutableList()

        rvFeed.adapter = PostAdapter(
            context = this,
            lista = listaGlobal,
            onEdit = {},      // obligatorio pero vacío
            onDelete = {}     // obligatorio pero vacío
        )
    }

    // ============================================================
    // OBTENER PUBLICACIONES
    // ============================================================
    private fun obtenerPublicacionesGlobales(): List<Post> {
        val lista = mutableListOf<Post>()

        for (key in sharedPref.all.keys) {
            if (key.endsWith("_posts")) {
                val raw = sharedPref.getStringSet(key, emptySet()) ?: emptySet()

                raw.forEach {
                    val parts = it.split("|")
                    if (parts.size == 4) {
                        lista.add(Post(parts[0], parts[1], parts[2], parts[3]))
                    }
                }
            }
        }

        return lista
    }

    // ============================================================
    // BUSCADOR
    // ============================================================
    private fun filtrarBuscador(query: String) {

        if (query.isBlank()) {
            rvFeed.adapter = PostAdapter(this, listaGlobal, onEdit = {}, onDelete = {})
            return
        }

        val filtrados = listaGlobal.filter { post ->
            post.tag.contains(query, ignoreCase = true) ||
                    post.contenido.contains(query, ignoreCase = true)
        }

        rvFeed.adapter = PostAdapter(this, filtrados, onEdit = {}, onDelete = {})
    }

    // ============================================================
    // PUBLICAR DESDE HOME
    // ============================================================
    private fun abrirDialogNuevaPublicacionHome() {

        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_new_post, null)

        val txtNombre = view.findViewById<TextView>(R.id.txtDialogNombre)
        val txtCarrera = view.findViewById<TextView>(R.id.txtDialogCarrera)
        val spinnerTags = view.findViewById<Spinner>(R.id.spinnerTags)
        val edtText = view.findViewById<EditText>(R.id.edtPostText)
        val btnPublicar = view.findViewById<Button>(R.id.btnPublicar)

        val data = sharedPref.getString(currentEmail, "")!!.split("#")

        val nombre = data[0]
        val carrera = data[2]
        val atributos = data.last()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        txtNombre.text = nombre
        txtCarrera.text = carrera

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, atributos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTags.adapter = adapter

        btnPublicar.setOnClickListener {

            val tag = spinnerTags.selectedItem.toString()
            val texto = edtText.text.toString().trim()

            if (texto.isBlank()) {
                Toast.makeText(this, "Escribe algo para publicar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarPublicacion(nombre, carrera, tag, texto)
            dialog.dismiss()

            listaGlobal = obtenerPublicacionesGlobales().toMutableList()
            rvFeed.adapter = PostAdapter(this, listaGlobal, onEdit = {}, onDelete = {})
        }

        dialog.setView(view)
        dialog.show()
    }

    // ============================================================
    // GUARDAR PUBLICACIÓN
    // ============================================================
    private fun guardarPublicacion(nombre: String, carrera: String, tag: String, texto: String) {

        val post = "$nombre|$carrera|$tag|$texto"
        val key = "${currentEmail}_posts"

        val current = sharedPref.getStringSet(key, mutableSetOf())!!.toMutableSet()
        current.add(post)

        sharedPref.edit().putStringSet(key, current).apply()

        Toast.makeText(this, "Publicado", Toast.LENGTH_SHORT).show()
    }
}
