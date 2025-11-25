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
        //val username = intent.getStringExtra("username") ?: "Usuario"
        val sharedPref = getSharedPreferences("NexuUsers", MODE_PRIVATE)
        val email = sharedPref.getString("currentUser", null) ?: ""

        // Obtener datos guardados del usuario
        val data = sharedPref.getString(email, null)

        var username = "Usuario"

        if (data != null) {
            val parts = data.split("#")
            username = parts.getOrNull(0) ?: "Usuario"
        }

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

        val messagesection : LinearLayout = findViewById(R. id.messagesection)
        messagesection.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}
