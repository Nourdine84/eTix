package com.etix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // ⏳ Attente de 3 secondes avant de lancer StartActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }, 3000)
    }
}
