package com.etix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.login.LoginActivity
import com.etix.ui.main.MainActivityV2
import com.etix.ui.onboarding.OnboardingActivity
import com.etix.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({

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
}
