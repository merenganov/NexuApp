package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream
import android.util.Base64

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var email: String
    private lateinit var txtAtributos: TextView
    private lateinit var boxAtributos: LinearLayout

    private val PICK_IMAGE = 100

    private val listaAtributos = listOf(
        "Amante de los animales", "Gamer", "Lector habitual", "Deportista", "Fitness",
        "Aficionado a la astronomía", "Coleccionista", "Bailarin", "Ambientalista",
        "Minimalista", "Creador de contenido", "Pintor", "Musico", "Diseñador grafico",
        "Chef", "Profesor", "Medico", "Abogado", "Emprendedor", "Enfermero",
        "Fotografo", "Progamador", "Escritor"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // 1️⃣ INICIALIZAR SharedPreferences y email ANTES DE USARLOS
        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)
        email = sharedPref.getString("currentUser", null) ?: ""

        // 2️⃣ REFERENCIAS UI
        txtAtributos = findViewById(R.id.txtAtributos)
        boxAtributos = findViewById(R.id.boxAtributos)
        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)

        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val edtCarrera = findViewById<EditText>(R.id.edtCarrera)
        val edtDescripcion = findViewById<EditText>(R.id.edtDescripcion)
        val edtFecha = findViewById<EditText>(R.id.edtFecha)
        val edtGenero = findViewById<EditText>(R.id.edtGenero)

        val btnEditar = findViewById<Button>(R.id.btnEditar)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)
        val msgCompletar = findViewById<TextView>(R.id.msgCompletar)
        val btnAddPost = findViewById<ImageView>(R.id.btnAddPost)

        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        // 3️⃣ CARGAR DATOS DEL USUARIO
        val data = sharedPref.getString(email, null)
        var nombre = ""
        var password = ""
        var carrera = ""
        var descripcion = ""
        var fecha = ""
        var genero = ""
        var atributos = ""

        if (data != null) {
            val parts = data.split("#")
            nombre = parts.getOrNull(0) ?: ""
            password = parts.getOrNull(1) ?: ""
            carrera = parts.getOrNull(2) ?: ""
            descripcion = parts.getOrNull(3) ?: ""
            fecha = parts.getOrNull(4) ?: ""
            genero = parts.getOrNull(5) ?: ""
            atributos = parts.getOrNull(6) ?: ""
        }

        // 4️⃣ MOSTRAR DATOS EN PANTALLA
        findViewById<TextView>(R.id.txtNombreHeader).text = nombre
        txtNombre.text = nombre
        edtCarrera.setText(carrera)
        edtDescripcion.setText(descripcion)
        edtFecha.setText(fecha)
        edtGenero.setText(genero)
        txtAtributos.text = atributos

        // Foto de perfil guardada (si existe)
        cargarFotoPerfil(imgPerfil)

        // Mensaje de perfil incompleto
        val incompleto = carrera.isBlank() || descripcion.isBlank() ||
                fecha.isBlank() || genero.isBlank() || atributos.isBlank()
        msgCompletar.visibility = if (incompleto) View.VISIBLE else View.GONE

        // Desactivar edición al inicio
        setEditable(false, edtCarrera, edtDescripcion, edtFecha, edtGenero)
        btnFinalizar.visibility = View.GONE

        // 5️⃣ BOTÓN EDITAR PERFIL
        btnEditar.setOnClickListener {
            setEditable(true, edtCarrera, edtDescripcion, edtFecha, edtGenero)

            txtAtributos.visibility = View.GONE
            boxAtributos.visibility = View.VISIBLE

            val guardados = atributos.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            cargarCheckboxAtributos(guardados)

            btnFinalizar.visibility = View.VISIBLE
            btnEditar.visibility = View.GONE
        }

        // 6️⃣ BOTÓN FINALIZAR EDICIÓN
        btnFinalizar.setOnClickListener {
            val newCarrera = edtCarrera.text.toString()
            val newDescripcion = edtDescripcion.text.toString()
            val newFecha = edtFecha.text.toString()
            val newGenero = edtGenero.text.toString()

            val seleccionados = mutableListOf<String>()
            for (i in 0 until boxAtributos.childCount) {
                val check = boxAtributos.getChildAt(i) as CheckBox
                if (check.isChecked) seleccionados.add(check.text.toString())
            }
            val newAtributos = seleccionados.joinToString(", ")

            val updatedData =
                "$nombre#$password#$newCarrera#$newDescripcion#$newFecha#$newGenero#$newAtributos"

            sharedPref.edit().putString(email, updatedData).apply()

            txtAtributos.text = newAtributos
            txtAtributos.visibility = View.VISIBLE
            boxAtributos.visibility = View.GONE

            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()

            btnFinalizar.visibility = View.GONE
            btnEditar.visibility = View.VISIBLE
            setEditable(false, edtCarrera, edtDescripcion, edtFecha, edtGenero)

            val incompletoFinal =
                newCarrera.isBlank() || newDescripcion.isBlank() ||
                        newFecha.isBlank() || newGenero.isBlank() || newAtributos.isBlank()
            msgCompletar.visibility = if (incompletoFinal) View.VISIBLE else View.GONE
        }

        // 7️⃣ FOTO DE PERFIL (subir / eliminar)
        imgPerfil.setOnClickListener {
            val opciones = arrayOf("Subir foto", "Eliminar foto")

            AlertDialog.Builder(this)
                .setTitle("Foto de perfil")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> abrirGaleria()
                        1 -> eliminarFotoPerfil(imgPerfil)
                    }
                }.show()
        }

        // 8️⃣ BOTÓN NUEVA PUBLICACIÓN
        btnAddPost.setOnClickListener {
            val partsUser = sharedPref.getString(email, "")!!.split("#")
            val atributosActuales = partsUser.last()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (atributosActuales.isEmpty()) {
                Toast.makeText(
                    this,
                    "Primero selecciona atributos en tu perfil",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            abrirDialogNuevaPublicacion(nombre, carrera, atributosActuales)
        }
        // ⭐ BOTÓN MENÚ SUPERIOR
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)

        btnMenu.setOnClickListener {
            val popup = PopupMenu(this, btnMenu)
            popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {

                    R.id.opCuenta -> {
                        // Aquí puedes agregar lógica si luego quieres editar cuenta
                    }

                    R.id.opTema -> {
                        val dialog = AlertDialog.Builder(this).create()
                        val view = layoutInflater.inflate(R.layout.theme_selector, null)

                        val switchTema = view.findViewById<Switch>(R.id.switchTema)
                        val imgTema = view.findViewById<ImageView>(R.id.imgTema)
                        val txtTema = view.findViewById<TextView>(R.id.txtTema)

                        val isDark = ThemeManager.isDark(this)
                        switchTema.isChecked = isDark

                        imgTema.setImageResource(if (isDark) R.drawable.ic_moon else R.drawable.ic_sun)
                        txtTema.text = if (isDark) "Modo claro" else "Modo oscuro"

                        switchTema.setOnCheckedChangeListener { _, checked ->
                            ThemeManager.setDark(this, checked)
                            dialog.dismiss()
                            recreate()
                        }

                        dialog.setView(view)
                        dialog.show()
                    }

                    R.id.opCerrar -> {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Cerrar sesión")
                        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?")

                        builder.setPositiveButton("Sí") { dialog, _ ->
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                            dialog.dismiss()
                        }

                        builder.setNegativeButton("No") { d, _ -> d.dismiss() }
                        builder.create().show()
                    }
                }
                true
            }

            popup.show()
        }


        // 9️⃣ NAV inferior
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navMessages).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
            finish()
        }

        // 🔟 PUBLICACIONES DEL USUARIO
        val rvPub = findViewById<RecyclerView>(R.id.rvPublicaciones)
        rvPub.layoutManager = LinearLayoutManager(this)
        rvPub.adapter = PostAdapter(
            context = this,
            lista = obtenerPublicacionesUsuario(),

            // Ya NO existe onEdit en el nuevo adapter, lo implementamos manualmente usando el menú
            onDelete = { post ->
                eliminarPublicacion(post)
            },

            // Qué hacer cuando el usuario toca una publicación dentro de su perfil
            // Aquí NO debe abrir chat porque son publicaciones propias
            onItemClick = { post ->
                // En el perfil, la acción natural es EDITAR la publicación cuando se toca
                editarPublicacion(post)
            }
        )

    }

    // ================================================================
    // FOTO DE PERFIL
    // ================================================================

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

            // Guardar imagen codificada en SharedPreferences
            guardarImagenEnShared(bitmap)

            // Mostrar en ImageView
            findViewById<ImageView>(R.id.imgPerfil).setImageBitmap(bitmap)
        }
    }

    private fun guardarImagenEnShared(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val bytes = baos.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)

        sharedPref.edit().putString("${email}_foto", base64).apply()
    }

    private fun cargarFotoPerfil(imgPerfil: ImageView) {
        val fotoBase64 = sharedPref.getString("${email}_foto", null) ?: return

        val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        imgPerfil.setImageBitmap(bitmap)
    }

    private fun eliminarFotoPerfil(imgPerfil: ImageView) {
        sharedPref.edit().remove("${email}_foto").apply()
        imgPerfil.setImageResource(R.drawable.ic_profile)
        Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show()
    }

    // ================================================================
    // PUBLICACIONES
    // ================================================================

    private fun guardarPublicacion(nombre: String, carrera: String, tag: String, texto: String) {
        // ahora también guardamos el email del autor
        val post = "$nombre|$carrera|$tag|$texto|$email"
        val key = "${email}_posts"
        val current = sharedPref.getStringSet(key, mutableSetOf())!!.toMutableSet()

        current.add(post)
        sharedPref.edit().putStringSet(key, current).apply()

        Toast.makeText(this, "Publicación creada", Toast.LENGTH_SHORT).show()
    }


    private fun obtenerPublicacionesUsuario(): List<Post> {
        val key = "${email}_posts"
        val raw = sharedPref.getStringSet(key, emptySet()) ?: emptySet()

        return raw.mapNotNull { s ->
            val p = s.split("|")
            when (p.size) {
                4 -> Post(
                    nombre = p[0],
                    carrera = p[1],
                    tag = p[2],
                    contenido = p[3],
                    emailAutor = email       // como es tu perfil, el autor eres tú
                )
                5 -> Post(
                    nombre = p[0],
                    carrera = p[1],
                    tag = p[2],
                    contenido = p[3],
                    emailAutor = p[4]
                )
                else -> null
            }
        }
    }


    private fun abrirDialogNuevaPublicacion(
        nombre: String,
        carrera: String,
        atributos: List<String>
    ) {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_new_post, null)

        val txtNombre = view.findViewById<TextView>(R.id.txtDialogNombre)
        val txtCarrera = view.findViewById<TextView>(R.id.txtDialogCarrera)
        val spinnerTags = view.findViewById<Spinner>(R.id.spinnerTags)
        val edtText = view.findViewById<EditText>(R.id.edtPostText)
        val btnPublicar = view.findViewById<Button>(R.id.btnPublicar)

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
            recargarPublicaciones()
        }

        dialog.setView(view)
        dialog.show()
    }

    private fun editarPublicacion(post: Post) {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_new_post, null)

        val edtText = view.findViewById<EditText>(R.id.edtPostText)
        val spinnerTags = view.findViewById<Spinner>(R.id.spinnerTags)
        val btnPublicar = view.findViewById<Button>(R.id.btnPublicar)

        edtText.setText(post.contenido)

        val data = sharedPref.getString(email, "")!!.split("#")
        val atributos = data.last()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, atributos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTags.adapter = adapter
        spinnerTags.setSelection(atributos.indexOf(post.tag))

        btnPublicar.setOnClickListener {
            val nuevoTexto = edtText.text.toString()
            val nuevoTag = spinnerTags.selectedItem.toString()
            actualizarPublicacion(post, nuevoTag, nuevoTexto)
            dialog.dismiss()
        }

        dialog.setView(view)
        dialog.show()
    }

    private fun actualizarPublicacion(post: Post, nuevoTag: String, nuevoTexto: String) {
        val key = "${email}_posts"
        val current = sharedPref.getStringSet(key, mutableSetOf())!!.toMutableSet()

        val vieja = "${post.nombre}|${post.carrera}|${post.tag}|${post.contenido}"
        val nueva = "${post.nombre}|${post.carrera}|$nuevoTag|$nuevoTexto"

        current.remove(vieja)
        current.add(nueva)

        sharedPref.edit().putStringSet(key, current).apply()
        recargarPublicaciones()
    }

    private fun eliminarPublicacion(post: Post) {
        val key = "${email}_posts"
        val current = sharedPref.getStringSet(key, mutableSetOf())!!.toMutableSet()

        val cadena = "${post.nombre}|${post.carrera}|${post.tag}|${post.contenido}"
        current.remove(cadena)

        sharedPref.edit().putStringSet(key, current).apply()
        recargarPublicaciones()
    }

    private fun recargarPublicaciones() {
        val rvPub = findViewById<RecyclerView>(R.id.rvPublicaciones)
        rvPub.adapter = PostAdapter(
            context = this,
            lista = obtenerPublicacionesUsuario(),
            onDelete = { post -> eliminarPublicacion(post) },
            onItemClick = { post -> editarPublicacion(post) }
        )

    }

    // ================================================================
    // UTILIDADES
    // ================================================================

    private fun setEditable(state: Boolean, vararg fields: EditText) {
        fields.forEach { it.isEnabled = state }
    }

    private fun cargarCheckboxAtributos(atributosSeleccionados: List<String>) {
        boxAtributos.removeAllViews()
        for (atributo in listaAtributos) {
            val check = CheckBox(this).apply {
                text = atributo
                setTextColor(resources.getColor(R.color.black))
                isChecked = atributosSeleccionados.contains(atributo)
            }
            boxAtributos.addView(check)
        }
    }
}
