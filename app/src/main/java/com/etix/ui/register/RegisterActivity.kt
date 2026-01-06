package com.etix.ui.register

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.R
import com.etix.ui.login.LoginActivity
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager
import com.etix.utils.setOnDrawableEndClickListener

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        // 🔒 GUARD STRICT : déjà connecté → Main
        if (session.isLoggedIn()) {
            startActivity(
                Intent(this, MainActivityV2::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        setContentView(R.layout.activity_register)

        val editUsername = findViewById<EditText>(R.id.editTextUsername)
        val editFullName = findViewById<EditText>(R.id.edtFullName)
        val editEmail = findViewById<EditText>(R.id.edtEmail)
        val editPassword = findViewById<EditText>(R.id.edtPassword)
        val editConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoLogin = findViewById<Button>(R.id.btnGoLogin)

        // 👁️ Toggle visibilité mots de passe
        setupPasswordToggle(editPassword)
        setupPasswordToggle(editConfirmPassword)

        btnRegister.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val fullName = editFullName.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString()
            val confirm = editConfirmPassword.text.toString()

            // 🧪 Validation minimale V2
            if (
                username.isEmpty() ||
                fullName.isEmpty() ||
                email.isEmpty() ||
                password.isEmpty() ||
                confirm.isEmpty()
            ) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ ISO iOS : inscription = session ouverte directe
            session.login(username)

            startActivity(
                Intent(this, MainActivityV2::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }

        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupPasswordToggle(editText: EditText) {
        var visible = false
        editText.setOnDrawableEndClickListener {
            visible = !visible
            editText.inputType =
                if (visible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            editText.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (visible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed,
                0
            )
            editText.setSelection(editText.text.length)
        }
    }
}
