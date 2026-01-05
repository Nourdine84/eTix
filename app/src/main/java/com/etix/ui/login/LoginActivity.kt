package com.etix.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.R
import com.etix.ui.main.MainActivityV2
import com.etix.ui.register.RegisterActivity
import com.etix.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val inputUsername = findViewById<EditText>(R.id.inputUsername)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)

        val session = SessionManager(this)

        btnLogin.setOnClickListener {
            val username = inputUsername.text.toString().trim()
            val password = inputPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Champs requis", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ LOGIN SIMPLE (Sprint 2.1)
            session.login(username)

            startActivity(
                Intent(this, MainActivityV2::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgot?.setOnClickListener {
            Toast.makeText(this, "À venir", Toast.LENGTH_SHORT).show()
        }
    }
}
