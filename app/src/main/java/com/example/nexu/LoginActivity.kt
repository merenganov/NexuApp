package com.example.nexu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nexu.R
import android.content.Intent   // Importa la clase Intent
import android.widget.Button   // Importa la clase Button


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val signinButton: Button = findViewById(R.id.signinButton)

        // Configurar el OnClickListener para abrir la actividad de Crear Cuenta
        signinButton.setOnClickListener {
            // Crear un Intent para redirigir a CreateAccountActivity
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
        }
    }
}
