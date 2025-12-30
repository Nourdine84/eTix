package com.etix

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.main.MainActivityV2 // ✅ Import correct

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val inputUsername = findViewById<EditText>(R.id.inputUsername)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoRegister = findViewById<Button>(R.id.btnGoRegister)

        btnLogin.setOnClickListener {
            val username = inputUsername.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPrefs: SharedPreferences = getSharedPreferences("eTixPrefs", MODE_PRIVATE)
            val registeredUsername = sharedPrefs.getString("username", null)
            val registeredPassword = sharedPrefs.getString("password", null)

            if (registeredUsername == username && registeredPassword == password) {
                with(sharedPrefs.edit()) {
                    putBoolean("isLoggedIn", true)
                    apply()
                }
                Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivityV2::class.java)) // ✅ V2 activée
                finish()
            } else {
                Toast.makeText(this, "Nom d’utilisateur ou mot de passe incorrect", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}
