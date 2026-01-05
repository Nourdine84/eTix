package com.etix.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.R
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

        // 👁️ toggle password
        var visible = false
        inputPassword.setOnDrawableEndClickListener {
            visible = !visible
            inputPassword.inputType =
                if (visible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            inputPassword.setSelection(inputPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val username = inputUsername.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Champs manquants", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            session.login(username)

            startActivity(Intent(this, MainActivityV2::class.java))
            finish()
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, com.etix.RegisterActivity::class.java))
        }
    }
}
