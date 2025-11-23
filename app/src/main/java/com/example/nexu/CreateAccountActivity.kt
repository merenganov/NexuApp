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

            // Validar correo institucional
            if (!isEmailValid(email)) {
                Toast.makeText(this, "Correo inválido, debe terminar con @edu.uaa.mx", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar contraseñas iguales
            if (!arePasswordsMatching(password, confirmPassword)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar datos en SharedPreferences
            saveUserData(name, email, password)

            // Volver al Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Validación de correo institucional
    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@edu.uaa.mx"
        return email.matches(emailPattern.toRegex())
    }

    // Validación de contraseñas
    private fun arePasswordsMatching(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    // Guardar usuario en SharedPreferences (FORMATO CORRECTO)
    private fun saveUserData(name: String, email: String, password: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("NexuUsers", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Guardar en formato: "nombre#contraseña"
        val userData = "$name#$password"

        // La clave será el email (IMPORTANTE)
        editor.putString(email, userData)

        editor.apply()
    }
}
