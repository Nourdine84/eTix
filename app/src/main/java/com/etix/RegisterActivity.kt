package com.etix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)
        val confirmPasswordInput = findViewById<EditText>(R.id.editTextConfirmPassword)
        val registerButton = findViewById<Button>(R.id.buttonRegister)
        val loginLink = findViewById<TextView>(R.id.textLogin)

        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            when {
                email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                    showMessage("Veuillez remplir tous les champs.")
                }

                password != confirmPassword -> {
                    showMessage("Les mots de passe ne correspondent pas.")
                }

                password.length < 6 -> {
                    showMessage("Le mot de passe doit contenir au moins 6 caractères.")
                }

                else -> {
                    // ✅ Inscription simulée réussie → redirige vers l'accueil
                    showMessage("Compte créé avec succès !")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
