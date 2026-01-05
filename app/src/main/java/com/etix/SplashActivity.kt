package com.etix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.login.LoginActivity
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager
// import com.etix.ui.onboarding.OnboardingActivity // 👉 prêt pour plus tard

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({

            val nextIntent = when {

                // 🧭 ONBOARDING (désactivé pour l’instant)
                /*
                !session.isOnboardingSeen() -> {
                    Intent(this, OnboardingActivity::class.java)
                }
                */

                // 🔐 Utilisateur déjà connecté
                session.isLoggedIn() -> {
                    Intent(this, MainActivityV2::class.java)
                }

                // 🔓 Cas par défaut
                else -> {
                    Intent(this, LoginActivity::class.java)
                }
            }

            startActivity(
                nextIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()

        }, 600)
    }
}
