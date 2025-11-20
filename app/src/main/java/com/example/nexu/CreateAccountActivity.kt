package com.example.nexu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val finalizeButton: Button = findViewById(R.id.finalizeButton)

        finalizeButton.setOnClickListener {
            val name = findViewById<EditText>(R.id.nameInput).text.toString()
            val email = findViewById<EditText>(R.id.emailInput).text.toString()
            val password = findViewById<EditText>(R.id.passwordInput).text.toString()
            val confirmPassword = findViewById<EditText>(R.id.confirmPasswordInput).text.toString()

            // Validar correo
            if (!isEmailValid(email)) {
                Toast.makeText(this, "Correo inválido, debe terminar con @edu.uaa.mx", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar contraseñas
            if (!arePasswordsMatching(password, confirmPassword)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar datos en SharedPreferences
            saveUserData(name, email, password)

            // Redirigir a la pantalla principal
            val intent = Intent(this, LoginActivity ::class.java)
            startActivity(intent)
        }
    }

    // Función para validar el correo
    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@edu.uaa.mx"  // Solo permite correos @edu.uaa.mx
        return email.matches(emailPattern.toRegex())
    }

    // Función para validar que las contraseñas coincidan
    private fun arePasswordsMatching(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    // Guardar datos de usuario en SharedPreferences
    private fun saveUserData(name: String, email: String, password: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("user_name", name)
        editor.putString("user_email", email)
        editor.putString("user_password", password)
        editor.apply()
    }
}
