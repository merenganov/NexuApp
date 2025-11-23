package com.example.nexu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // === Referencias a los elementos del layout ===
        val signinButton: Button = findViewById(R.id.signinButton)
        val loginButton: Button = findViewById(R.id.loginButton)
        val emailInput: EditText = findViewById(R.id.emailInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)

        // === BOTÓN PARA IR A CREAR CUENTA ===
        signinButton.setOnClickListener {
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
        }

        // === BOTÓN LOGIN (validar usuario) ===
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cargar usuarios almacenados
            val sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)
            val storedData = sharedPref.getString(email, null)

            if (storedData == null) {
                Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // storedData viene como: nombre#contraseña
            val parts = storedData.split("#")
            val storedName = parts[0]
            val storedPassword = parts[1]

            if (storedPassword != password) {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // === LOGIN EXITOSO ===
            Toast.makeText(this, "Bienvenido $storedName", Toast.LENGTH_SHORT).show()

            val homeIntent = Intent(this, HomeActivity::class.java)
            homeIntent.putExtra("username", storedName)
            startActivity(homeIntent)
        }
    }
}

