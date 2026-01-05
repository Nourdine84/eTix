package com.etix.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.R
import com.etix.ui.main.MainActivityV2
import com.etix.ui.register.RegisterActivity
import com.etix.utils.SessionManager
import com.etix.utils.setOnDrawableEndClickListener

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        // ✅ Auto-skip si déjà connecté
        if (session.isLoggedIn()) {
            startActivity(
                Intent(this, MainActivityV2::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val inputUsername = findViewById<EditText>(R.id.inputUsername)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)

        // 🔘 Activation dynamique bouton
        fun updateButtonState() {
            val enabled =
                inputUsername.text.isNotBlank() && inputPassword.text.isNotBlank()
            btnLogin.isEnabled = enabled
            btnLogin.alpha = if (enabled) 1f else 0.65f
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        inputUsername.addTextChangedListener(watcher)
        inputPassword.addTextChangedListener(watcher)

        // 👁️ Toggle visibilité mot de passe
        var isPasswordVisible = false
        inputPassword.setOnDrawableEndClickListener {
            isPasswordVisible = !isPasswordVisible
            inputPassword.inputType =
                if (isPasswordVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            inputPassword.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (isPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed,
                0
            )
            inputPassword.setSelection(inputPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val username = inputUsername.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(this, "Champs requis", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Login Sprint 2.1
            session.login(username)

            startActivity(
                Intent(this, MainActivityV2::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgot.setOnClickListener {
            Toast.makeText(this, "Fonction à venir", Toast.LENGTH_SHORT).show()
        }
    }
}
