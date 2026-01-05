package com.etix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.login.LoginActivity
import com.etix.ui.main.MainActivityV2
import com.etix.ui.register.RegisterActivity   // ✅ IMPORT MANQUANT
import com.etix.utils.SessionManager

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        // 🔐 GUARD : si déjà connecté → Main
        if (session.isLoggedIn()) {
            startActivity(
                Intent(this, MainActivityV2::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        setContentView(R.layout.activity_welcome)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
