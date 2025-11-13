package com.example.nexu

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Imagen del logo
        val logoImage = findViewById<ImageView>(R.id.logo_image)

        // Cargar animación (fade in)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        logoImage.startAnimation(fadeIn)

        // Reproducir el audio (efect.mp3)
        val mediaPlayer = MediaPlayer.create(this, R.raw.noti2)
        mediaPlayer.start()

        // Después de 3.5 segundos pasar al MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            // Detener y liberar el audio
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()

            // Ir al MainActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 5000) // Duración total del splash (igual al fade in + audio)
    }
}
