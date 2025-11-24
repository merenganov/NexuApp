package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var email: String
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)

        // Traemos el email del usuario actual
        email = intent.getStringExtra("email") ?: ""



        // Referencias UI
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val edtCarrera = findViewById<EditText>(R.id.edtCarrera)
        val edtDescripcion = findViewById<EditText>(R.id.edtDescripcion)
        val edtFecha = findViewById<EditText>(R.id.edtFecha)
        val edtGenero = findViewById<EditText>(R.id.edtGenero)
        val edtAtributos = findViewById<EditText>(R.id.edtAtributos)

        val btnEditar = findViewById<Button>(R.id.btnEditar)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)
        val msgCompletar = findViewById<TextView>(R.id.msgCompletar)

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
        val txtNombreHeader = findViewById<TextView>(R.id.txtNombreHeader)
        txtNombreHeader.text = nombre   // nombre viene del split de SharedPreferences


        // Mostrar datos
        txtNombre.text = nombre
        edtCarrera.setText(carrera)
        edtDescripcion.setText(descripcion)
        edtFecha.setText(fecha)
        edtGenero.setText(genero)
        edtAtributos.setText(atributos)

        // Mostrar mensaje si todo está vacío
        val perfilIncompleto =
            carrera.isBlank() ||
                    descripcion.isBlank() ||
                    fecha.isBlank() ||
                    genero.isBlank() ||
                    atributos.isBlank()

        msgCompletar.visibility = if (perfilIncompleto) View.VISIBLE else View.GONE



        // ========================================
        // DESACTIVAR EDICIÓN AL INICIO
        // ========================================
        setEditable(false, edtCarrera, edtDescripcion, edtFecha, edtGenero, edtAtributos)
        btnFinalizar.visibility = View.GONE

        // ========================================
        // BOTÓN EDITAR
        // ========================================
        btnEditar.setOnClickListener {
            setEditable(true, edtCarrera, edtDescripcion, edtFecha, edtGenero, edtAtributos)
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
            val newAtributos = edtAtributos.text.toString()

            // Guardar actualización
            val updatedData =
                "$nombre#$password#$newCarrera#$newDescripcion#$newFecha#$newGenero#$newAtributos"

            sharedPref.edit().putString(email, updatedData).apply()

            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()

            btnFinalizar.visibility = View.GONE
            btnEditar.visibility = View.VISIBLE
            setEditable(false, edtCarrera, edtDescripcion, edtFecha, edtGenero, edtAtributos)

            // Ocultar mensaje si ya tiene datos
            val perfilIncompletoFinal =
                newCarrera.isBlank() ||
                        newDescripcion.isBlank() ||
                        newFecha.isBlank() ||
                        newGenero.isBlank() ||
                        newAtributos.isBlank()

            msgCompletar.visibility = if (perfilIncompletoFinal) View.VISIBLE else View.GONE


        }
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        //val navMessages = findViewById<LinearLayout>(R.id.navMessages)
        //val navProfile = findViewById<LinearLayout>(R.id.navProfile)
    }

    // Función para activar/desactivar edición
    private fun setEditable(state: Boolean, vararg fields: EditText) {
        for (field in fields) {
            field.isEnabled = state
        }
    }

}
