package com.etix

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.etix.data.getDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketDetailActivity : AppCompatActivity() {

    private lateinit var tvStoreName: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        // Liaison avec le layout XML
        tvStoreName = findViewById(R.id.tvDetailStore)
        tvAmount = findViewById(R.id.tvDetailAmount)
        tvCategory = findViewById(R.id.tvDetailCategory)
        tvDate = findViewById(R.id.tvDetailDate)
        tvDescription = findViewById(R.id.tvDetailDescription)

        val ticketId = intent.getLongExtra("ticketId", -1)

        if (ticketId != -1L) {
            val dao = getDatabase(this).ticketDao()

            lifecycleScope.launch {
                val ticket = withContext(Dispatchers.IO) {
                    dao.getById(ticketId) // ✅ Méthode DAO correcte
                }

                if (ticket != null) {
                    tvStoreName.text = ticket.store          // ✅ Champ correct
                    tvAmount.text = "Montant : ${ticket.amount} €"
                    tvCategory.text = "Catégorie : ${ticket.category}"
                    tvDate.text = "Date : ${formatDate(ticket.dateMillis)}"
                    tvDescription.text = "Description : ${ticket.description ?: "—"}"
                } else {
                    Toast.makeText(this@TicketDetailActivity, "Ticket introuvable", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "ID du ticket manquant", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun formatDate(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}