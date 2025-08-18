package com.etix

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redirige vers la page de connexion dès le lancement
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
