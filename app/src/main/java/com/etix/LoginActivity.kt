package com.etix

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.main.MainActivityV2
import com.etix.utils.setOnDrawableEndClickListener


class LoginActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "etix_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val inputUsername = findViewById<EditText>(R.id.inputUsername)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // 👁️ Toggle mot de passe
        var isPasswordVisible = false
        inputPassword.setOnDrawableEndClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                inputPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                inputPassword.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.ic_eye_open, 0
                )
            } else {
                inputPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                inputPassword.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.ic_eye_closed, 0
                )
            }
            inputPassword.setSelection(inputPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val username = inputUsername.text.toString().trim()
            val password = inputPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedUsername = prefs.getString(KEY_USERNAME, null)
            val savedPassword = prefs.getString(KEY_PASSWORD, null)

            if (username == savedUsername && password == savedPassword) {
                prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
                startActivity(Intent(this, MainActivityV2::class.java))
                finish()
            } else {
                Toast.makeText(this, "Identifiants incorrects", Toast.LENGTH_SHORT).show()
            }
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
