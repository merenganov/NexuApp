package com.example.nexu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // === Referencias a los elementos del layout ===
        val signinButton: Button = findViewById(R.id.signinButton)
        val loginButton: Button = findViewById(R.id.loginButton)
        val emailInput: EditText = findViewById(R.id.emailInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)

        // SharedPreferences para guardar token y usuario
        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)

        // === BOTÓN PARA IR A CREAR CUENTA ===
        signinButton.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        // === BOTÓN LOGIN QUE AHORA LLAMA AL BACKEND ===
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hacemos login real hacia el backend
            hacerLogin(email, password)
        }
    }

    // ================================================================
    // FUNCIÓN QUE SE CONECTA AL BACKEND PARA VALIDAR LOGIN
    // ================================================================
    private fun hacerLogin(email: String, password: String) {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, password)

                // Petición al backend
                val response = RetrofitClient.api.login(request)

                withContext(Dispatchers.Main) {

                    if (response.isSuccessful) {
                        val data = response.body()?.data

                        if (data != null) {

                            val token = data.accessToken
                            val userId = data.userId

                            // Guardamos token y usuario actual en SharedPreferences
                            sharedPref.edit()
                                .putString("token", token)
                                .putString("currentUser", email)
                                .putString("currentUserId", userId)
                                .apply()

                            Toast.makeText(
                                this@LoginActivity,
                                "Inicio de sesión exitoso",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Ir a HomeActivity
                            val homeIntent = Intent(this@LoginActivity, HomeActivity::class.java)
                            homeIntent.putExtra("email", email)
                            startActivity(homeIntent)
                            finish()

                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Error al procesar los datos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Credenciales incorrectas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error al conectar con el servidor: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
