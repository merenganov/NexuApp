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
import kotlinx.coroutines.*

class HomeActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var rvFeed: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var jwtToken: String
    private lateinit var currentUserId: String

    private var listaGlobal: MutableList<Post> = mutableListOf()

    private val api = RetrofitClient.api

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ThemeManager.applyThemeBackground(this, findViewById(android.R.id.content))

        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)

        jwtToken = sharedPref.getString("token", "") ?: ""
        currentUserId = sharedPref.getString("user_id", "") ?: ""

        if (jwtToken.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Navegación inferior
        initBottomNav()

        // RecyclerView
        rvFeed = findViewById(R.id.rvFeed)
        rvFeed.layoutManager = LinearLayoutManager(this)

        postAdapter = PostAdapter(
            this,
            listaGlobal,
            onDelete = { post -> confirmarYEliminar(post) },
            onItemClick = { post ->
                if (post.user.id != currentUserId) {
                    startActivity(Intent(this, ChatActivity::class.java).apply {
                        putExtra("emailOtro", post.user.name)
                        putExtra("nombreOtro", post.user.name)
                    })
                } else {
                    Toast.makeText(this, "Esta es tu publicación", Toast.LENGTH_SHORT).show()
                }
            }
        )

        rvFeed.adapter = postAdapter

        // Botón para publicar
        findViewById<EditText>(R.id.newPostInput).setOnClickListener {
            abrirDialogNuevaPublicacionHome()
        }

        // Buscador
        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarBuscador(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarFeedGlobal()
    }

    // ============================================================
    // OBTENER FEED GLOBAL DESDE EL BACKEND
    // ============================================================
    private fun cargarFeedGlobal() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = api.getPosts("Bearer $jwtToken")

                if (response.isSuccessful) {
                    listaGlobal = response.body()?.data?.toMutableList() ?: mutableListOf()
                    postAdapter.setPosts(listaGlobal)
                } else {
                    Toast.makeText(this@HomeActivity, "Error en servidor", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ============================================================
    // FILTRO DE BUSQUEDA POR TAGS
    // ============================================================
    private fun filtrarBuscador(query: String) {
        if (query.isBlank()) {
            postAdapter.setPosts(listaGlobal)
            return
        }

        val filtrados = listaGlobal.filter { post ->
            post.tag.name.contains(query, ignoreCase = true) ||
                    post.description.contains(query, ignoreCase = true)
        }

        postAdapter.setPosts(filtrados)
    }

    // ============================================================
    // PUBLICAR DESDE HOME (CON TAGS REALES)
    // ============================================================
    private fun abrirDialogNuevaPublicacionHome() {

        val view = layoutInflater.inflate(R.layout.dialog_create_post, null)
        val spinner = view.findViewById<Spinner>(R.id.spinnerTags)
        val edtDesc = view.findViewById<EditText>(R.id.edtDescripcionPost)

        CoroutineScope(Dispatchers.Main).launch {

            try {
                val response = api.getTags("Bearer $jwtToken")

                if (response.isSuccessful) {
                    val tags = response.body()?.data ?: emptyList()
                    val nombres = tags.map { it.name }

                    val adapter = ArrayAdapter(
                        this@HomeActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        nombres
                    )
                    spinner.adapter = adapter

                    AlertDialog.Builder(this@HomeActivity)
                        .setTitle("Nueva publicación")
                        .setView(view)
                        .setPositiveButton("Publicar") { _, _ ->

                            val desc = edtDesc.text.toString().trim()
                            if (desc.isBlank()) {
                                Toast.makeText(this@HomeActivity, "Escribe algo", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }

                            val tagSeleccionado = tags[spinner.selectedItemPosition]
                            crearPost(tagSeleccionado.id, desc)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Error cargando tags", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ============================================================
    // CREAR POST (BACKEND)
    // ============================================================
    private fun crearPost(tagId: String, desc: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = api.createPost("Bearer $jwtToken", CreatePostRequest(tagId, desc))

                if (response.isSuccessful) {
                    cargarFeedGlobal()
                }

            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ============================================================
    // ELIMINAR POST DESDE HOME
    // ============================================================
    private fun confirmarYEliminar(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar publicación")
            .setMessage("¿Seguro que quieres eliminar esta publicación?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarPost(post)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPost(post: Post) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = api.deletePost("Bearer $jwtToken", post.id)

                if (response.isSuccessful) {
                    listaGlobal.remove(post)
                    postAdapter.removePostById(post.id)
                }

            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Error eliminando", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ============================================================
    // NAVEGACIÓN INFERIOR
    // ============================================================
    private fun initBottomNav() {
        findViewById<LinearLayout>(R.id.messagesection).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.profileSection).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)); finish()
        }
    }
}
