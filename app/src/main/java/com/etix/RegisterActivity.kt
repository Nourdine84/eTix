package com.etix

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.utils.setOnDrawableEndClickListener

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "etix_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_REGISTERED = "is_registered"
    }

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

        val prefs: SharedPreferences =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // 👁️ Toggle mot de passe
        var isPasswordVisible = false
        editPassword.setOnDrawableEndClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(editPassword, isPasswordVisible)
        }

        // 👁️ Toggle confirmation mot de passe
        var isConfirmPasswordVisible = false
        editConfirmPassword.setOnDrawableEndClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(editConfirmPassword, isConfirmPasswordVisible)
        }

        btnRegister.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val fullName = editFullName.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()

            if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            with(prefs.edit()) {
                putString(KEY_USERNAME, username)
                putString(KEY_EMAIL, email)
                putString(KEY_PASSWORD, password)
                putBoolean(KEY_IS_REGISTERED, true)
                apply()
            }

            Toast.makeText(this, "Inscription réussie !", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun togglePasswordVisibility(editText: EditText, visible: Boolean) {
        if (visible) {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_eye_open, 0
            )
        } else {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_eye_closed, 0
            )
        }
        editText.setSelection(editText.text.length)
    }
}
