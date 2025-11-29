package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var email: String
    private lateinit var txtAtributos: TextView
    private lateinit var boxAtributos: LinearLayout

    private val listaAtributos = listOf(
        "Amante de los animales",
        "Gamer",
        "Lector habitual",
        "Deportista",
        "Fitness",
        "Aficionado a la astronomía",
        "Coleccionista",
        "Bailarin",
        "Ambientalista",
        "Minimalista",
        "Creador de contenido",
        "Pintor",
        "Musico",
        "Diseñador grafico",
        "Chef",
        "Profesor",
        "Medico",
        "Abogado",
        "Emprendedor",
        "Enfermero",
        "Fotografo",
        "Progamador",
        "Escritor",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        txtAtributos = findViewById(R.id.txtAtributos)
        boxAtributos = findViewById(R.id.boxAtributos)

        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)
        email = sharedPref.getString("currentUser", null) ?: ""

        // Referencias UI
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val edtCarrera = findViewById<EditText>(R.id.edtCarrera)
        val edtDescripcion = findViewById<EditText>(R.id.edtDescripcion)
        val edtFecha = findViewById<EditText>(R.id.edtFecha)
        val edtGenero = findViewById<EditText>(R.id.edtGenero)

        val btnEditar = findViewById<Button>(R.id.btnEditar)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)
        val msgCompletar = findViewById<TextView>(R.id.msgCompletar)
        val btnAddPost = findViewById<ImageView>(R.id.btnAddPost)


        // ================================
        //   CARGAR DATOS EXISTENTES
        // ================================
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

        findViewById<TextView>(R.id.txtNombreHeader).text = nombre

        // Mostrar datos
        txtNombre.text = nombre
        edtCarrera.setText(carrera)
        edtDescripcion.setText(descripcion)
        edtFecha.setText(fecha)
        edtGenero.setText(genero)
        txtAtributos.text = atributos   // 🔥 Aquí va el PASSO 8

        // Mostrar mensaje si está incompleto
        val perfilIncompleto =
            carrera.isBlank() ||
                    descripcion.isBlank() ||
                    fecha.isBlank() ||
                    genero.isBlank() ||
                    atributos.isBlank()

        msgCompletar.visibility = if (perfilIncompleto) View.VISIBLE else View.GONE

        // DESACTIVAR EDICIÓN
        setEditable(false, edtCarrera, edtDescripcion, edtFecha, edtGenero)
        btnFinalizar.visibility = View.GONE

        // ========================================
        // BOTÓN EDITAR
        // ========================================
        btnEditar.setOnClickListener {

            setEditable(true, edtCarrera, edtDescripcion, edtFecha, edtGenero)

            txtAtributos.visibility = View.GONE
            boxAtributos.visibility = View.VISIBLE

            val datos = sharedPref.getString(email, "")!!.split("#")
            val guardados = datos.last()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            cargarCheckboxAtributos(guardados)

            btnFinalizar.visibility = View.VISIBLE
            btnEditar.visibility = View.GONE
        }

        // ========================================
        // BOTÓN FINALIZAR
        // ========================================
        btnFinalizar.setOnClickListener {

            val newCarrera = edtCarrera.text.toString()
            val newDescripcion = edtDescripcion.text.toString()
            val newFecha = edtFecha.text.toString()
            val newGenero = edtGenero.text.toString()

            // Obtener atributos seleccionados
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

            val perfilIncompletoFinal =
                newCarrera.isBlank() ||
                        newDescripcion.isBlank() ||
                        newFecha.isBlank() ||
                        newGenero.isBlank() ||
                        newAtributos.isBlank()

            msgCompletar.visibility = if (perfilIncompletoFinal) View.VISIBLE else View.GONE
        }

        //BOTON PAR AÑADIR PUBLICACION
        btnAddPost.setOnClickListener {

            val dataUser = sharedPref.getString(email, "")!!.split("#")
            val atributosActuales = dataUser.last(

            )

            abrirDialogNuevaPublicacion(nombre, carrera, atributosActuales)
        }



        // NAV, POPUP, ETC (Tu código sigue igual)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        btnMenu.setOnClickListener {
            val popup = PopupMenu(this, btnMenu)
            popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.opCuenta -> {}
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
                        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        builder.create().show()
                    }
                }
                true
            }
            popup.show()
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navMessages).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
            finish()
        }

        val rvPub = findViewById<RecyclerView>(R.id.rvPublicaciones)
        rvPub.layoutManager = LinearLayoutManager(this)

        val posts = obtenerPublicacionesUsuario()
        rvPub.adapter = PostAdapter(
            this,
            posts,
            onEdit = { editarPublicacion(it) },
            onDelete = { eliminarPublicacion(it) }
        )


    }

    private fun setEditable(state: Boolean, vararg fields: EditText) {
        for (field in fields) field.isEnabled = state
    }

    private fun cargarCheckboxAtributos(atributosSeleccionados: List<String>) {
        boxAtributos.removeAllViews()
        for (atributo in listaAtributos) {
            val check = CheckBox(this)
            check.text = atributo
            check.setTextColor(resources.getColor(R.color.black))

            if (atributosSeleccionados.contains(atributo)) check.isChecked = true
            boxAtributos.addView(check)
        }
    }
    private fun abrirDialogNuevaPublicacion(nombre: String, carrera: String, atributos: String) {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_new_post, null)

        val txtNombre = view.findViewById<TextView>(R.id.txtDialogNombre)
        val txtCarrera = view.findViewById<TextView>(R.id.txtDialogCarrera)
        val spinnerTags = view.findViewById<Spinner>(R.id.spinnerTags)
        val edtText = view.findViewById<EditText>(R.id.edtPostText)
        val btnPublicar = view.findViewById<Button>(R.id.btnPublicar)

        txtNombre.text = nombre
        txtCarrera.text = carrera

        // 🔥 OBTENER ATRIBUTOS REALES DEL USUARIO DESDE SharedPreferences
        val data = sharedPref.getString(email, "")!!.split("#")
        val atributosGuardados = data.last()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 🔥 LLENAR SPINNER CON ESOS ATRIBUTOS
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, atributosGuardados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTags.adapter = adapter

        btnPublicar.setOnClickListener {

            val tag = spinnerTags.selectedItem?.toString() ?: ""
            val texto = edtText.text.toString()

            if (texto.isBlank()) {
                Toast.makeText(this, "Escribe algo para publicar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarPublicacion(nombre, carrera, tag, texto)

            dialog.dismiss()

            // 🔥 RECARGAR LAS PUBLICACIONES DEL PERFIL
            val rvPub = findViewById<RecyclerView>(R.id.rvPublicaciones)
            rvPub.adapter = PostAdapter(
                this,
                obtenerPublicacionesUsuario(),
                onEdit = { editarPublicacion(it) },
                onDelete = { eliminarPublicacion(it) }
            )

        }

        dialog.setView(view)
        dialog.show()
    }


    private fun guardarPublicacion(nombre: String, carrera: String, tag: String, texto: String) {

        // Formato único de la publicación
        val post = "$nombre|$carrera|$tag|$texto"

        // Clave para este usuario
        val key = "${email}_posts"

        // Recuperar publicaciones existentes
        val current = sharedPref.getStringSet(key, mutableSetOf())!!.toMutableSet()

        // Agregar la nueva
        current.add(post)

        // Guardar de nuevo el set
        sharedPref.edit().putStringSet(key, current).apply()

        Toast.makeText(this, "Publicación creada", Toast.LENGTH_SHORT).show()
    }



    private fun obtenerPublicacionesUsuario(): List<Post> {
        val key = "${email}_posts"
        val raw = sharedPref.getStringSet(key, emptySet()) ?: emptySet()

        val lista = mutableListOf<Post>()

        for (p in raw) {
            val parts = p.split("|")
            if (parts.size == 4) {
                lista.add(Post(
                    nombre = parts[0],
                    carrera = parts[1],
                    tag = parts[2],
                    contenido = parts[3]
                ))
            }
        }

        return lista
    }

    private fun editarPublicacion(post: Post) {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_new_post, null)

        val edtText = view.findViewById<EditText>(R.id.edtPostText)
        val spinnerTags = view.findViewById<Spinner>(R.id.spinnerTags)
        val btnPublicar = view.findViewById<Button>(R.id.btnPublicar)

        edtText.setText(post.contenido)

        val data = sharedPref.getString(email, "")!!.split("#")
        val atributos = data.last().split(",").map { it.trim() }.filter { it.isNotBlank() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, atributos)
        spinnerTags.adapter = adapter

        spinnerTags.setSelection(atributos.indexOf(post.tag))

        btnPublicar.setOnClickListener {
            val nuevoTexto = edtText.text.toString()
            val nuevoTag = spinnerTags.selectedItem.toString()
            actualizarPublicacion(post, nuevoTag, nuevoTexto)   //
            dialog.dismiss()
        }


        dialog.setView(view)
        dialog.show()
    }

    private fun actualizarPublicacion(post: Post, nuevoTag: String, nuevoTexto: String) {
        val key = "${email}_posts"
        val current = sharedPref.getStringSet(key, mutableSetOf())!!.toMutableSet()

        // Cadena vieja (como está guardada ahorita)
        val vieja = "${post.nombre}|${post.carrera}|${post.tag}|${post.contenido}"
        // Cadena nueva actualizada
        val nueva = "${post.nombre}|${post.carrera}|$nuevoTag|$nuevoTexto"

        // Reemplazar en el set
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
            this,
            obtenerPublicacionesUsuario(),
            onEdit = { editarPublicacion(it) },
            onDelete = { eliminarPublicacion(it) }
        )
    }



}
