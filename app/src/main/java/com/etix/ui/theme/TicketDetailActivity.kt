package com.etix.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.etix.R

class TicketDetailActivity : AppCompatActivity(R.layout.activity_ticket_detail) {

    companion object {
        private const val EXTRA_ID = "ticket_id"

        fun start(context: Context, id: Long) {
            context.startActivity(Intent(context, TicketDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, id)
            })
        }

        fun readId(intent: Intent): Long = intent.getLongExtra(EXTRA_ID, -1L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // le fragment récupérera l'id depuis l'Activity intent
    }
}
