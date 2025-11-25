package com.example.nexu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        // Obtener el nombre enviado desde LoginActivity
        val username = intent.getStringExtra("username") ?: "Usuario"

        // Referencia al TextView
        val welcomeText: TextView = findViewById(R.id.welcomeText)

        // Colocar el mensaje personalizado
        welcomeText.text = "Bienvenid@ $username"

        val profileSection: LinearLayout = findViewById(R.id.profileSection)

        profileSection.setOnClickListener {
            val email = intent.getStringExtra("email") ?: ""
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
            finish()
        }

    }
}
