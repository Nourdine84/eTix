package com.etix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.main.MainActivityV2

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ⚠️ PAS de setContentView → splash géré par le thème

        Handler(Looper.getMainLooper()).postDelayed({

            val isLoggedIn = getSharedPreferences("etix_prefs", MODE_PRIVATE)
                .getBoolean("is_logged_in", false)

            val nextActivity = if (isLoggedIn) {
                MainActivityV2::class.java
            } else {
                LoginActivity::class.java
            }

            startActivity(Intent(this, nextActivity))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()

        }, 600) // 600ms = fluide, premium, non intrusif
    }
}
