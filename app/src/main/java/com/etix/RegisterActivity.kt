package com.etix

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        fun updateRegisterButtonState() {
            val ok = editUsername.text.toString().trim().isNotEmpty()
                    && editFullName.text.toString().trim().isNotEmpty()
                    && editEmail.text.toString().trim().isNotEmpty()
                    && editPassword.text.toString().isNotEmpty()
                    && editConfirmPassword.text.toString().isNotEmpty()
            btnRegister.isEnabled = ok
            btnRegister.alpha = if (ok) 1.0f else 0.65f
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateRegisterButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        editUsername.addTextChangedListener(watcher)
        editFullName.addTextChangedListener(watcher)
        editEmail.addTextChangedListener(watcher)
        editPassword.addTextChangedListener(watcher)
        editConfirmPassword.addTextChangedListener(watcher)
        updateRegisterButtonState()

        // 👁️ Toggle password
        var passVisible = false
        editPassword.setOnDrawableEndClickListener {
            passVisible = !passVisible
            if (passVisible) {
                editPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                editPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_open, 0)
            } else {
                editPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_closed, 0)
            }
            editPassword.setSelection(editPassword.text.length)
        }

        // 👁️ Toggle confirm password
        var confirmVisible = false
        editConfirmPassword.setOnDrawableEndClickListener {
            confirmVisible = !confirmVisible
            if (confirmVisible) {
                editConfirmPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                editConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_open, 0)
            } else {
                editConfirmPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_closed, 0)
            }
            editConfirmPassword.setSelection(editConfirmPassword.text.length)
        }

        fun doRegister() {
            val username = editUsername.text.toString().trim()
            val fullName = editFullName.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()

            if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                return
            }

            hideKeyboard()

            prefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .putBoolean(KEY_IS_REGISTERED, true)
                .apply()

            Toast.makeText(this, "Inscription réussie !", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        editConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doRegister()
                true
            } else false
        }

        btnRegister.setOnClickListener { doRegister() }

        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        if (view != null) imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
