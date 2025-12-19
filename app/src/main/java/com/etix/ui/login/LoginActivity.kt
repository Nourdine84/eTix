package com.etix.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.etix.R
import com.etix.ui.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister) // si bouton secondaire
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            // TODO: auth
        }

        btnRegister?.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgot?.setOnClickListener {
            // TODO: ouvrir écran reset mdp
        }
    }
}