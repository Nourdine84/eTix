package com.etix

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager
import com.etix.utils.setOnDrawableEndClickListener

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val session = SessionManager(this)

        val inputUsername = findViewById<EditText>(R.id.inputUsername)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)

        // 👁️ Toggle mot de passe
        var isPasswordVisible = false
        inputPassword.setOnDrawableEndClickListener {
            isPasswordVisible = !isPasswordVisible
            inputPassword.inputType =
                if (isPasswordVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            inputPassword.setSelection(inputPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val username = inputUsername.text.toString().trim()
            val password = inputPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ⛳ Login local simple (V1)
            session.login(username, password)

            startActivity(Intent(this, MainActivityV2::class.java))
            finish()
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
