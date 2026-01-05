package com.etix

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.etix.ui.login.LoginActivity
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        val next = if (session.isLoggedIn()) {
            Intent(this, MainActivityV2::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        startActivity(next)
        finish()
    }
}
