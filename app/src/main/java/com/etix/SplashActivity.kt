// 📁 com.etix.SplashActivity.kt
package com.etix

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Délai de 2 secondes avant de rediriger
        Handler(Looper.getMainLooper()).postDelayed({

            val sharedPrefs: SharedPreferences = getSharedPreferences("eTixPrefs", MODE_PRIVATE)
            val isLoggedIn = sharedPrefs.getBoolean("isLoggedIn", false)

            // Redirection selon session
            val nextActivity = if (isLoggedIn) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, nextActivity))
            finish()

        }, 2000)
    }
}