package com.example.nexu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val finalizeButton: Button = findViewById(R.id.finalizeButton)

        finalizeButton.setOnClickListener {
            val name = findViewById<EditText>(R.id.nameInput).text.toString().trim()
            val email = findViewById<EditText>(R.id.emailInput).text.toString().trim()
            val password = findViewById<EditText>(R.id.passwordInput).text.toString().trim()
            val confirmPassword = findViewById<EditText>(R.id.confirmPasswordInput).text.toString().trim()

            // Validar campos vacíos
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar correo institucional
            if (!isEmailValid(email)) {
                Toast.makeText(this, "Correo inválido, debe terminar con @edu.uaa.mx", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar contraseñas iguales
            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Crear request para el backend
            val request = SignupRequest(
                name = name,
                email = email,
                password = password,
                gender = "Masculino"  // Ajustado a tu backend
            )

            registrarUsuario(request)
        }
    }

    // ====================================================
    //  FUNCIÓN QUE CONECTA CON EL BACKEND / SIGNUP
    // ====================================================
    private fun registrarUsuario(request: SignupRequest) {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.signup(request)

                withContext(Dispatchers.Main) {
                    when {
                        response.isSuccessful -> {
                            Toast.makeText(
                                this@CreateAccountActivity,
                                "Cuenta creada correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Redirigir al Login
                            startActivity(Intent(this@CreateAccountActivity, LoginActivity::class.java))
                            finish()
                        }

                        response.code() == 409 -> {
                            Toast.makeText(
                                this@CreateAccountActivity,
                                "Ya existe una cuenta con ese correo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Toast.makeText(
                                this@CreateAccountActivity,
                                "Error al registrar usuario",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CreateAccountActivity,
                        "Error al conectar con el servidor: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Validación de correo institucional
    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@edu.uaa.mx"
        return email.matches(emailPattern.toRegex())
    }
}
