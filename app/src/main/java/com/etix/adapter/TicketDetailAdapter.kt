package com.etix.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.Ticket

class TicketDetailAdapter(
    private var tickets: List<Ticket>
) : RecyclerView.Adapter<TicketDetailAdapter.TicketViewHolder>() {

    inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStoreName: TextView = itemView.findViewById(R.id.tvStoreName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket_detail, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = tickets[position]
        holder.tvStoreName.text = ticket.store
        holder.tvAmount.text = "Montant : %.2f €".format(ticket.amount)
        holder.tvDate.text = "Date : " + formatDate(ticket.dateMillis)
        holder.tvCategory.text = "Catégorie : " + ticket.category
        holder.tvDescription.text =
            if (!ticket.description.isNullOrBlank()) "Description : ${ticket.description}"
            else "Description : -"
    }

    override fun getItemCount(): Int = tickets.size

    fun updateTickets(newTickets: List<Ticket>) {
        this.tickets = newTickets
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy")
        return sdf.format(java.util.Date(timestamp))
    }
}