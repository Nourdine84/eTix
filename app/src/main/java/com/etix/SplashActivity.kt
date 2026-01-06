package com.etix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.etix.ui.login.LoginActivity
import com.etix.ui.main.MainActivityV2
import com.etix.ui.onboarding.OnboardingActivity
import com.etix.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {

        // ✅ FORCER LE MODE THEME AVANT TOUT (évite les mélanges)
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        handler.postDelayed({

            // 🛡️ Sécurité lifecycle
            if (isFinishing || isDestroyed) return@postDelayed

            val nextIntent = when {

                // 🧭 FIRST LAUNCH → ONBOARDING
                session.isFirstLaunch() -> {
                    Intent(this, OnboardingActivity::class.java)
                }

                // 🔐 Déjà connecté
                session.isLoggedIn() -> {
                    Intent(this, MainActivityV2::class.java)
                }

                // 🔓 Par défaut
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
