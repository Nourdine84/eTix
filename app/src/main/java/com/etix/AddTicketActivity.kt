package com.etix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.etix.R

class AddTicketActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_add_ticket) // ⚠️ Réutilise le XML déjà présent
    }
}
