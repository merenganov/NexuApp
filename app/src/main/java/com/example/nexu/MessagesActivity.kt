package com.example.nexu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MessagesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Aplicar el tema (claro/oscuro)
        val root = findViewById<View>(android.R.id.content)
        ThemeManager.applyThemeBackground(this, root)

        val navHomeM = findViewById<LinearLayout>(R.id.navHomeM)
        navHomeM.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
        val navProfileM = findViewById<LinearLayout>(R.id.navProfileM)
        navProfileM.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
