package com.etix.ui.register

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.etix.R

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val fullName = findViewById<EditText>(R.id.edtFullName)
        val email = findViewById<EditText>(R.id.edtEmail)
        val pass = findViewById<EditText>(R.id.edtPassword)
        val confirm = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoLogin = findViewById<Button>(R.id.btnGoLogin)

        btnRegister.setOnClickListener {
            val name = fullName.text.toString().trim()
            val mail = email.text.toString().trim()
            val p1 = pass.text.toString()
            val p2 = confirm.text.toString()

            if (name.isEmpty() || mail.isEmpty() || p1.isEmpty() || p2.isEmpty()) {
                Toast.makeText(this, R.string.msg_fill_all, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (p1 != p2) {
                Toast.makeText(this, R.string.msg_password_mismatch, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: plug API/DB here
            Toast.makeText(this, R.string.msg_registered_demo, Toast.LENGTH_SHORT).show()
            finish() // revient au Login
        }

        btnGoLogin.setOnClickListener { finish() }
    }
}