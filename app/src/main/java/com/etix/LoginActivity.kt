package com.etix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val textRegister = findViewById<TextView>(R.id.textRegister)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // ✅ Connexion réussie → redirige vers l’accueil
                Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // ⚠️ Champs vides
                Toast.makeText(this, "Remplis tous les champs", Toast.LENGTH_SHORT).show()
            }
        }

        textRegister.setOnClickListener {
            // 🔁 Ouvre l’écran d’inscription
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
