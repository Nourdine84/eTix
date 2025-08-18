package com.etix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome) // ✅ ici on charge le bon layout

        val btnAddTicket = findViewById<Button>(R.id.btnAddTicket)

        btnAddTicket.setOnClickListener {
            val intent = Intent(this, AddTicketActivity::class.java)
            startActivity(intent)
        }
    }
}
