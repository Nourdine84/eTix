package com.etix.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.Ticket
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TicketHistoryAdapter(
    private var items: List<Ticket>
) : RecyclerView.Adapter<TicketHistoryAdapter.ViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    fun submitList(newItems: List<Ticket>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStore = view
            .findViewById<TextView>(R.id.tvStore)
        private val tvAmount = view.findViewById<TextView>(R.id.tvAmount)
        private val tvDate = view.findViewById<TextView>(R.id.tvDate)

        fun bind(ticket: Ticket) {
            tvStore.text = ticket.store
            tvAmount.text = NumberFormat.getCurrencyInstance(Locale.FRANCE).format(ticket.amount)
            tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                .format(Date(ticket.dateMillis))
        }
    }
}
