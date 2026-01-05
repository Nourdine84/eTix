package com.etix.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.etix.R
import com.etix.ui.login.LoginActivity
import com.etix.utils.SessionManager

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val session = SessionManager(this)
        val btnStart = findViewById<Button>(R.id.btnStart)

        btnStart.setOnClickListener {
            session.setOnboardingSeen()

            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }
}
