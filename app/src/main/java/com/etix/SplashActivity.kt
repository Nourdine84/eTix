package com.etix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ⚠️ PAS de setContentView → splash géré par le thème

        val session = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({

            val nextActivity = if (session.isLoggedIn()) {
                MainActivityV2::class.java
            } else {
                LoginActivity::class.java
            }

            startActivity(Intent(this, nextActivity))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()

        }, 600) // fluide, premium, non intrusif
    }
}
