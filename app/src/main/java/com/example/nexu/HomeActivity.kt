package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nexu.sockets.SocketManager
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

        cargarNombreUsuario()
        // Inicializamos el socket
        lifecycleScope.launch(Dispatchers.IO) {
            SocketManager.initialize(applicationContext, jwtToken)
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
//                    Revisar si no se tenia ya chat con esa persona

                    startActivity(Intent(this, ChatActivity::class.java).apply {
                        putExtra("other_id", post.user.id)
                        putExtra("ifFirst", true)
                        putExtra("nombreOtro", post.user.name)
                    })
                } else {
                    Toast.makeText(this, "Esta es tu publicación", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvFeed.adapter = postAdapter

        // NUEVA PUBLICACIÓN
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = api.getPosts("Bearer $jwtToken")

                if (response.isSuccessful) {
                    listaGlobal = response.body()?.data?.toMutableList() ?: mutableListOf()

                    withContext(Dispatchers.Main) {
                        postAdapter.setPosts(listaGlobal)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HomeActivity, "Error en servidor", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ============================================================
    // FILTRO DE BUSQUEDA POR TAGS O DESCRIPCIÓN
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
    // CREAR PUBLICACIÓN DESDE HOME
    // ============================================================
    private fun abrirDialogNuevaPublicacionHome() {

        val view = layoutInflater.inflate(R.layout.dialog_create_post, null)
        val spinnerTags = view.findViewById<Spinner>(R.id.spinnerTags)
        val edtDesc = view.findViewById<EditText>(R.id.edtDescripcionPost)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val res = api.getTags("Bearer $jwtToken")

                if (res.isSuccessful) {
                    val tags = res.body()?.data ?: emptyList()

                    withContext(Dispatchers.Main) {
                        spinnerTags.adapter = ArrayAdapter(
                            this@HomeActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            tags.map { it.name }
                        )

                        AlertDialog.Builder(this@HomeActivity)
                            .setTitle("Nueva publicación")
                            .setView(view)
                            .setPositiveButton("Publicar") { _, _ ->

                                val desc = edtDesc.text.toString().trim()
                                if (desc.isBlank()) {
                                    Toast.makeText(this@HomeActivity, "Escribe algo...", Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }

                                val tagId = tags[spinnerTags.selectedItemPosition].id
                                crearPost(tagId, desc)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "Error cargando tags", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ============================================================
    // CREAR POST (BACKEND)
    // ============================================================
    private fun crearPost(tagId: String, desc: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val body = CreatePostRequest(tag_id = tagId, description = desc)
                val res = api.createPost("Bearer $jwtToken", body)

                if (res.isSuccessful) {
                    cargarFeedGlobal()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = api.deletePost("Bearer $jwtToken", post.id)

                if (response.isSuccessful) {
                    listaGlobal.remove(post)

                    withContext(Dispatchers.Main) {
                        postAdapter.removePostById(post.id)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "Error eliminando", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ============================================================
    // NAVEGACIÓN
    // ============================================================
    private fun initBottomNav() {
        findViewById<LinearLayout>(R.id.messagesection).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.profileSection).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)); finish()
        }
    }

    // ============================================================
    // MOSTRAR NOMBRE EN EL HOME
    // ============================================================
    private fun cargarNombreUsuario() {
        val token = sharedPref.getString("token", null) ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val res = api.getUserProfile("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        val nombre = res.body()?.data?.name ?: "Usuario"
                        findViewById<TextView>(R.id.welcomeText).text = "Bienvenido, $nombre!"
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.welcomeText).text = "Bienvenido!"
                }
            }
        }
    }
}
