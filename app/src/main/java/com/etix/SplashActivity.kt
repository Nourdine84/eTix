// 📁 com.etix.SplashActivity.kt
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

        val session = SessionManager(this)

        // ✅ APPLY THEME BEFORE ANY UI
        AppCompatDelegate.setDefaultNightMode(session.getThemeMode())

        super.onCreate(savedInstanceState)

        handler.postDelayed({

            if (isFinishing || isDestroyed) return@postDelayed

            val nextIntent = when {

                // 🧭 FIRST LAUNCH → ONBOARDING
                session.isFirstLaunch() -> {
                    Intent(this, OnboardingActivity::class.java)
                }

                // 🔐 USER LOGGED
                session.isLoggedIn() -> {
                    Intent(this, MainActivityV2::class.java)
                }

                // 🔓 DEFAULT
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
