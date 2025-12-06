package com.example.nexu

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var token: String? = null

    private var currentUserProfile: UserProfile? = null
    private var allTags: List<Tag> = emptyList()
    private val tagNameToId = mutableMapOf<String, String>()

    private val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // SharedPreferences
        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)
        token = sharedPref.getString("token", null)

        if (token == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Fondo dinámico
        ThemeManager.applyThemeBackground(this, findViewById(android.R.id.content))

        // 🔹 LISTENERS QUE DEBEN EJECUTARSE SOLO UNA VEZ
        configurarListenersUnaVez()

        // 🔹 Cargar perfil desde backend
        cargarPerfilYTags()
    }

    // =================================================================
    //  LISTENERS QUE SOLO SE CONFIGURAN UNA VEZ (BOTONES, MENÚ, ETC)
    // =================================================================
    private fun configurarListenersUnaVez() {

        // Botón Editar Perfil
        findViewById<Button>(R.id.btnEditar).setOnClickListener {
            entrarEnModoEdicion()
        }

        // Botón Finalizar
        findViewById<Button>(R.id.btnFinalizar).setOnClickListener {
            guardarCambios()
        }

        // Botón subir foto
        findViewById<ImageView>(R.id.imgPerfil).setOnClickListener {
            seleccionarFoto()
        }

        // Menú superior
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

        // Navegación inferior
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
    // CARGAR PERFIL Y TAGS DESDE BACKEND
    // =================================================================
    private fun cargarPerfilYTags() {

        val t = token ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val profileRes = RetrofitClient.api.getUserProfile("Bearer $t")
                val tagsRes = RetrofitClient.api.getTags("Bearer $t")

                if (profileRes.isSuccessful && tagsRes.isSuccessful) {

                    currentUserProfile = profileRes.body()?.data
                    allTags = tagsRes.body()?.data ?: emptyList()

                    tagNameToId.clear()
                    allTags.forEach { tag -> tagNameToId[tag.name] = tag.id }

                    withContext(Dispatchers.Main) {
                        mostrarPerfilEnUI(currentUserProfile!!)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error cargando datos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // =================================================================
    // MOSTRAR LOS DATOS DEL PERFIL EN PANTALLA
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
        

        // Mensaje de perfil incompleto
        val incompleto = (
                profile.career.isNullOrBlank()
                        || profile.bio.isNullOrBlank()
                        || profile.date_of_birth.isNullOrBlank()
                        || profile.gender.isNullOrBlank()
                        || atributosTxt.isBlank()
                )

        findViewById<TextView>(R.id.msgCompletar).visibility =
            if (incompleto) View.VISIBLE else View.GONE


        // Asegurar que está en modo vista
        salirDeModoEdicion()
    }

    // =================================================================
    // ENTRAR EN MODO EDICIÓN
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

    // =================================================================
    // SALIR DEL MODO EDICIÓN
    // =================================================================
    private fun salirDeModoEdicion() {

        setEditable(false)

        findViewById<EditText>(R.id.edtGenero).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.boxGenero).visibility = View.GONE

        findViewById<TextView>(R.id.txtAtributos).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.boxAtributos).visibility = View.GONE

        findViewById<Button>(R.id.btnFinalizar).visibility = View.GONE
        findViewById<Button>(R.id.btnEditar).visibility = View.VISIBLE
    }

    // =================================================================
    // HABILITAR / DESHABILITAR CAMPOS EDITABLES
    // =================================================================
    private fun setEditable(enable: Boolean) {
        val campos = listOf(
            findViewById<EditText>(R.id.edtCarrera),
            findViewById<EditText>(R.id.edtDescripcion),
            findViewById<EditText>(R.id.edtFecha)
        )
        campos.forEach { it.isEnabled = enable }
    }

    // =================================================================
    // CHECKBOX DE GÉNERO (solo uno seleccionado)
    // =================================================================
    private fun prepararCheckboxGenero() {

        val checkM = findViewById<CheckBox>(R.id.checkMasculino)
        val checkF = findViewById<CheckBox>(R.id.checkFemenino)
        val checkO = findViewById<CheckBox>(R.id.checkOtro)

        val grupo = listOf(checkM, checkF, checkO)

        grupo.forEach { cb ->
            cb.isChecked = false
            cb.setOnCheckedChangeListener { _, checked ->
                if (checked) grupo.filter { it != cb }.forEach { it.isChecked = false }
            }
        }
    }

    // =================================================================
    // CHECKBOX DE ATRIBUTOS
    // =================================================================
    private fun cargarCheckboxAtributos() {
        val layout = findViewById<LinearLayout>(R.id.boxAtributos)
        layout.removeAllViews()

        val seleccionados = currentUserProfile?.tags ?: emptyList()

        for (tag in allTags) {
            val check = CheckBox(this)
            check.text = tag.name
            check.isChecked = seleccionados.contains(tag.name)
            layout.addView(check)
        }
    }

    // =================================================================
    // GUARDAR CAMBIOS (PUT /users/me)
    // =================================================================
    private fun guardarCambios() {

        val carrera = findViewById<EditText>(R.id.edtCarrera).text.toString()
        val bio = findViewById<EditText>(R.id.edtDescripcion).text.toString()
        val fecha = findViewById<EditText>(R.id.edtFecha).text.toString()

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

        val t = token ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.api.updateUserProfile("Bearer $t", req)

                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        currentUserProfile = res.body()?.data
                        mostrarPerfilEnUI(currentUserProfile!!)
                        Toast.makeText(this@ProfileActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Error actualizando perfil", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
            { _, y, m, d ->
                findViewById<EditText>(R.id.edtFecha).setText(
                    "$y-${(m + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, "0"[0])}"
                )
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    // =================================================================
    // SUBIR FOTO AL BACKEND
    // =================================================================
    private fun seleccionarFoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            subirFotoAlBackend(uri)
        }
    }

    private fun subirFotoAlBackend(uri: Uri) {

        val t = token ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return@launch

                val reqFile: RequestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())

                val body = MultipartBody.Part.createFormData(
                    "avatar",
                    "profile.jpg",
                    reqFile
                )

                val res = RetrofitClient.api.uploadAvatar("Bearer $t", body)

                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        currentUserProfile = res.body()?.data
                        mostrarPerfilEnUI(currentUserProfile!!)
                        Toast.makeText(this@ProfileActivity, "Foto actualizada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Error al subir foto", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
