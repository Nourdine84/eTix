package com.etix.ui.register

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.R
import com.etix.ui.login.LoginActivity
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Champs
        val edtUsername = findViewById<EditText>(R.id.editTextUsername)
        val edtFullName = findViewById<EditText>(R.id.edtFullName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)

        // Boutons
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoLogin = findViewById<Button>(R.id.btnGoLogin)

        val sessionManager = SessionManager(this)

        // Activation dynamique du bouton (UX propre, sans design)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val enabled =
                    edtUsername.text.isNotBlank() &&
                            edtFullName.text.isNotBlank() &&
                            edtEmail.text.isNotBlank() &&
                            edtPassword.text.isNotBlank() &&
                            edtConfirmPassword.text.isNotBlank()

                btnRegister.isEnabled = enabled
                btnRegister.alpha = if (enabled) 1f else 0.65f
            }
        }

        edtUsername.addTextChangedListener(watcher)
        edtFullName.addTextChangedListener(watcher)
        edtEmail.addTextChangedListener(watcher)
        edtPassword.addTextChangedListener(watcher)
        edtConfirmPassword.addTextChangedListener(watcher)

        // Action inscription
        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString()
            val confirm = edtConfirmPassword.text.toString()

            if (password != confirm) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sprint 2.1 : session directe
            sessionManager.login(username)

            startActivity(
                Intent(this, MainActivityV2::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

        // Retour login
        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
