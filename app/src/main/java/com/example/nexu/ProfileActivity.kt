package com.example.nexu

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var api: ApiService

    private lateinit var jwtToken: String
    private lateinit var currentUserId: String

    private var currentUserProfile: UserProfile? = null
    private var allTags: List<TagPostData> = emptyList()
    private val tagNameToId = mutableMapOf<String, String>()

    private val PICK_IMAGE = 100

    private lateinit var rvPublicaciones: androidx.recyclerview.widget.RecyclerView
    private lateinit var postAdapter: PostAdapter


    // =================================================================
    // ON CREATE
    // =================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)
        api = RetrofitClient.api

        jwtToken = sharedPref.getString("token", "") ?: ""
        currentUserId = sharedPref.getString("user_id", "") ?: ""

        if (jwtToken.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        ThemeManager.applyThemeBackground(this, findViewById(android.R.id.content))

        configurarListenersUnaVez()
        cargarPerfilYTags()

        // RecyclerView de publicaciones
        rvPublicaciones = findViewById(R.id.rvPublicaciones)
        postAdapter = PostAdapter(
            this,
            mutableListOf(),
            onDelete = { post -> confirmarYEliminar(post) },
            onItemClick = {}
        )
        rvPublicaciones.layoutManager = LinearLayoutManager(this)
        rvPublicaciones.adapter = postAdapter

        // Botón "+"
        findViewById<ImageView>(R.id.btnAddPost).setOnClickListener {
            mostrarDialogCrearPost()
        }

        cargarMisPosts()
    }


    // =================================================================
    // LISTENERS FIJOS (botones, menú, nav)
    // =================================================================
    private fun configurarListenersUnaVez() {

        // Botón Editar
        findViewById<Button>(R.id.btnEditar).setOnClickListener {
            entrarEnModoEdicion()
        }

        // Botón Finalizar
        findViewById<Button>(R.id.btnFinalizar).setOnClickListener {
            guardarCambios()
        }

        // Foto de perfil
        findViewById<ImageView>(R.id.imgPerfil).setOnClickListener {
            seleccionarFoto()
        }

        // Menú superior (tema / cerrar sesión)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        btnMenu.setOnClickListener {
            val popup = PopupMenu(this, btnMenu)
            popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.opTema -> {
                        val isDark = ThemeManager.isDark(this)
                        ThemeManager.setDark(this, !isDark)
                        recreate()
                    }
                    R.id.opCerrar -> {
                        AlertDialog.Builder(this)
                            .setTitle("Cerrar sesión")
                            .setMessage("¿Deseas salir?")
                            .setPositiveButton("Sí") { _, _ ->
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }
                }
                true
            }
            popup.show()
        }

        // NAV inferior
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navMessages).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
            finish()
        }
    }


    // =================================================================
    // CARGAR PERFIL + TAGS
    // =================================================================
    private fun cargarPerfilYTags() {
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val profileRes = api.getUserProfile("Bearer $jwtToken")
                val tagsRes = api.getTags("Bearer $jwtToken")

                if (profileRes.isSuccessful && tagsRes.isSuccessful) {

                    currentUserProfile = profileRes.body()?.data
                    allTags = tagsRes.body()?.data ?: emptyList<TagPostData>()

                    tagNameToId.clear()
                    allTags.forEach { tagNameToId[it.name] = it.id }

                    withContext(Dispatchers.Main) {
                        currentUserProfile?.let { mostrarPerfilEnUI(it) }
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Error al cargar perfil",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // =================================================================
    // MOSTRAR PERFIL EN UI
    // =================================================================
    private fun mostrarPerfilEnUI(profile: UserProfile) {

        findViewById<TextView>(R.id.txtNombreHeader).text = profile.name
        findViewById<TextView>(R.id.txtNombre).text = profile.name

        findViewById<EditText>(R.id.edtCarrera).setText(profile.career ?: "")
        findViewById<EditText>(R.id.edtDescripcion).setText(profile.bio ?: "")
        findViewById<EditText>(R.id.edtFecha).setText(profile.date_of_birth ?: "")
        findViewById<EditText>(R.id.edtGenero).setText(profile.gender ?: "")

        val atributosTxt = profile.tags?.joinToString(", ") ?: ""
        findViewById<TextView>(R.id.txtAtributos).text = atributosTxt

        val img = findViewById<ImageView>(R.id.imgPerfil)
        if (!profile.avatar_url.isNullOrBlank()) {
            Glide.with(this)
                .load(profile.avatar_url)
                .placeholder(R.drawable.ic_profile)
                .into(img)
        }

        // Mensaje de "termina tu perfil"
        val incompleto =
            profile.career.isNullOrBlank() ||
                    profile.bio.isNullOrBlank() ||
                    profile.date_of_birth.isNullOrBlank() ||
                    profile.gender.isNullOrBlank() ||
                    atributosTxt.isBlank()

        findViewById<TextView>(R.id.msgCompletar).visibility =
            if (incompleto) View.VISIBLE else View.GONE

        salirDeModoEdicion()
    }


    // =================================================================
    // POSTS DEL USUARIO
    // =================================================================
    private fun cargarMisPosts() {
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val res = api.getPosts("Bearer $jwtToken")

                if (!res.isSuccessful) return@launch

                val posts = res.body()?.data ?: emptyList()

                val filtrados = posts.filter { it.user.id == currentUserId }

                withContext(Dispatchers.Main) {
                    postAdapter.setPosts(filtrados)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // =================================================================
    // CREAR POST
    // =================================================================
    private fun mostrarDialogCrearPost() {

        val view = layoutInflater.inflate(R.layout.dialog_create_post, null)
        val spinner = view.findViewById<Spinner>(R.id.spinnerTags)
        val edtDesc = view.findViewById<EditText>(R.id.edtDescripcionPost)

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val tRes = api.getTags("Bearer $jwtToken")

                if (tRes.isSuccessful) {
                    val tags = tRes.body()?.data ?: emptyList()
                    val nombres = tags.map { it.name }

                    withContext(Dispatchers.Main) {
                        spinner.adapter = ArrayAdapter(
                            this@ProfileActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            nombres
                        )

                        AlertDialog.Builder(this@ProfileActivity)
                            .setTitle("Nueva publicación")
                            .setView(view)
                            .setPositiveButton("Publicar") { _, _ ->
                                val desc = edtDesc.text.toString().trim()
                                if (desc.isBlank()) {
                                    Toast.makeText(
                                        this@ProfileActivity,
                                        "Escribe algo",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@setPositiveButton
                                }
                                crearPost(tags[spinner.selectedItemPosition].id, desc)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }

                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun crearPost(tagId: String, desc: String) {

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val body = CreatePostRequest(tag_id = tagId, description = desc)
                val res = api.createPost("Bearer $jwtToken", body)

                if (res.isSuccessful) {
                    cargarMisPosts()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Publicado 🎉",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // =================================================================
    // ELIMINAR POST
    // =================================================================
    private fun confirmarYEliminar(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Eliminar esta publicación?")
            .setPositiveButton("Sí") { _, _ -> eliminarPost(post) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPost(post: Post) {

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val res = api.deletePost("Bearer $jwtToken", post.id)

                if (res.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        postAdapter.removePostById(post.id)
                        Toast.makeText(
                            this@ProfileActivity,
                            "Publicación eliminada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // =================================================================
    // SUBIR FOTO
    // =================================================================
    private fun seleccionarFoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            data?.data?.let { subirFotoAlBackend(it) }
        }
    }

    private fun subirFotoAlBackend(uri: Uri) {

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val bytes = contentResolver.openInputStream(uri)!!.readBytes()
                val reqFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("avatar", "profile.jpg", reqFile)

                val res = api.uploadAvatar("Bearer $jwtToken", body)

                if (res.isSuccessful) {
                    currentUserProfile = res.body()?.data

                    withContext(Dispatchers.Main) {
                        Glide.with(this@ProfileActivity)
                            .load(currentUserProfile?.avatar_url)
                            .placeholder(R.drawable.ic_profile)
                            .into(findViewById(R.id.imgPerfil))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        cargarPerfilYTags()
        cargarMisPosts()
    }


    // =================================================================
    // MODO EDICIÓN
    // =================================================================
    private fun entrarEnModoEdicion() {
        setEditable(true)
        findViewById<EditText>(R.id.edtGenero).visibility = View.GONE
        findViewById<LinearLayout>(R.id.boxGenero).visibility = View.VISIBLE
        prepararCheckboxGenero()

        findViewById<TextView>(R.id.txtAtributos).visibility = View.GONE
        findViewById<LinearLayout>(R.id.boxAtributos).visibility = View.VISIBLE
        cargarCheckboxAtributos()

        findViewById<Button>(R.id.btnEditar).visibility = View.GONE
        findViewById<Button>(R.id.btnFinalizar).visibility = View.VISIBLE
        findViewById<EditText>(R.id.edtFecha).setOnClickListener { abrirDatePicker() }
    }

    private fun salirDeModoEdicion() {
        setEditable(false)
        findViewById<EditText>(R.id.edtGenero).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.boxGenero).visibility = View.GONE
        findViewById<TextView>(R.id.txtAtributos).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.boxAtributos).visibility = View.GONE
        findViewById<Button>(R.id.btnFinalizar).visibility = View.GONE
        findViewById<Button>(R.id.btnEditar).visibility = View.VISIBLE
    }

    private fun setEditable(enable: Boolean) {
        val campos = listOf(
            findViewById<EditText>(R.id.edtCarrera),
            findViewById<EditText>(R.id.edtDescripcion),
            findViewById<EditText>(R.id.edtFecha),
        )
        campos.forEach { it.isEnabled = enable }
    }

    private fun prepararCheckboxGenero() {
        val group = listOf(
            findViewById<CheckBox>(R.id.checkMasculino),
            findViewById<CheckBox>(R.id.checkFemenino),
            findViewById<CheckBox>(R.id.checkOtro)
        )
        group.forEach { cb ->
            cb.isChecked = false
            cb.setOnCheckedChangeListener { _, checked ->
                if (checked) group.filter { it != cb }.forEach { it.isChecked = false }
            }
        }
    }

    private fun cargarCheckboxAtributos() {
        val layout = findViewById<LinearLayout>(R.id.boxAtributos)
        layout.removeAllViews()
        val sel = currentUserProfile?.tags ?: emptyList()
        for (tag in allTags) {
            val c = CheckBox(this)
            c.text = tag.name
            c.isChecked = sel.contains(tag.name)
            layout.addView(c)
        }
    }

    // =================================================================
    // GUARDAR CAMBIOS DE PERFIL
    // =================================================================
    private fun guardarCambios() {

        val carrera = findViewById<EditText>(R.id.edtCarrera).text.toString()
        val bio = findViewById<EditText>(R.id.edtDescripcion).text.toString()
        val fecha = findViewById<EditText>(R.id.edtFecha).text.toString()

        // Validar fecha ISO YYYY-MM-DD
        val isoRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
        if (!fecha.matches(isoRegex)) {
            Toast.makeText(
                this,
                "Formato de fecha inválido. Usa YYYY-MM-DD",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val genero =
            when {
                findViewById<CheckBox>(R.id.checkMasculino).isChecked -> "Masculino"
                findViewById<CheckBox>(R.id.checkFemenino).isChecked -> "Femenino"
                findViewById<CheckBox>(R.id.checkOtro).isChecked -> "Otro"
                else -> ""
            }

        val box = findViewById<LinearLayout>(R.id.boxAtributos)
        val seleccionados = mutableListOf<String>()

        for (i in 0 until box.childCount) {
            val cb = box.getChildAt(i) as CheckBox
            if (cb.isChecked) seleccionados.add(cb.text.toString())
        }

        val ids = seleccionados.mapNotNull { tagNameToId[it] }

        val req = UpdateProfileRequest(
            career = carrera,
            bio = bio,
            date_of_birth = fecha,
            gender = genero,
            tag_ids = ids
        )

        if (jwtToken.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val res = api.updateUserProfile("Bearer $jwtToken", req)

                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        currentUserProfile = res.body()?.data
                        currentUserProfile?.let { mostrarPerfilEnUI(it) }
                        Toast.makeText(
                            this@ProfileActivity,
                            "Perfil actualizado",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Error actualizando perfil",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // =================================================================
    // DATE PICKER
    // =================================================================
    private fun abrirDatePicker() {
        val cal = Calendar.getInstance()

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val mm = (month + 1).toString().padStart(2, '0')
                val dd = dayOfMonth.toString().padStart(2, '0')
                val fechaISO = "$year-$mm-$dd"   // YYYY-MM-DD

                findViewById<EditText>(R.id.edtFecha).setText(fechaISO)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }
}
