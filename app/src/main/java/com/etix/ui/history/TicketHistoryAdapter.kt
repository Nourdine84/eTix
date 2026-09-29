package com.etix.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.Ticket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketHistoryAdapter(
    private val onTicketClick: (Ticket) -> Unit = {}
) : RecyclerView.Adapter<TicketHistoryAdapter.TicketViewHolder>() {

    private val items = mutableListOf<Ticket>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    fun submitList(list: List<Ticket>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket_history, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvStore: TextView = itemView.findViewById(R.id.tvStore)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(ticket: Ticket) {
            tvStore.text = ticket.store
            tvAmount.text = String.format(Locale.FRANCE, "%.2f €", ticket.amount)
            tvDate.text = dateFormat.format(Date(ticket.dateMillis))
            itemView.setOnClickListener { onTicketClick(ticket) }
        }
    }
}
