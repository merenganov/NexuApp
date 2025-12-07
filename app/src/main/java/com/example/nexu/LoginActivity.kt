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

        val signinButton: Button = findViewById(R.id.signinButton)
        val loginButton: Button = findViewById(R.id.loginButton)
        val emailInput: EditText = findViewById(R.id.emailInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)

        sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)

        signinButton.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hacerLogin(email, password)
        }
    }

    // ================================================================
    // LOGIN REAL
    // ================================================================
    private fun hacerLogin(email: String, password: String) {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, password)
                val response = RetrofitClient.api.login(request)

                if (response.isSuccessful) {

                    val loginData = response.body()?.data
                    val token = loginData?.accessToken

                    if (token == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Token inválido", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Guardamos token temporalmente
                    sharedPref.edit()
                        .putString("token", token)
                        .apply()

                    // 🔥 AHORA OBLIGATORIAMENTE obtenemos el perfil real
                    obtenerPerfilDespuesDeLogin(token, email)

                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ================================================================
    // OBTENER PERFIL REAL (AQUÍ LLEGA EL user.id)
    // ================================================================
    private fun obtenerPerfilDespuesDeLogin(token: String, email: String) {

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val res = RetrofitClient.api.getUserProfile("Bearer $token")

                if (res.isSuccessful) {
                    val profile = res.body()?.data

                    if (profile != null) {

                        // 🔥 Guardamos el ID real del usuario
                        sharedPref.edit()
                            .putString("user_id", profile.id)
                            .putString("currentUser", email)
                            .apply()

                        withContext(Dispatchers.Main) {

                            Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                            // Ir al Home
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "No se pudo obtener el perfil", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
