package com.etix

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val editUsername = findViewById<EditText>(R.id.editTextUsername)
        val editFullName = findViewById<EditText>(R.id.edtFullName)
        val editEmail = findViewById<EditText>(R.id.edtEmail)
        val editPassword = findViewById<EditText>(R.id.edtPassword)
        val editConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoLogin = findViewById<Button>(R.id.btnGoLogin)

        btnRegister.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val fullName = editFullName.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()

            // Vérification basique
            if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sauvegarde du nom d'utilisateur (ou autre logique)
            val sharedPrefs: SharedPreferences = getSharedPreferences("eTixPrefs", MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString("username", username)
                putBoolean("isRegistered", true)
                apply()
            }

            Toast.makeText(this, "Inscription réussie !", Toast.LENGTH_SHORT).show()

            // Redirection vers Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}