package com.etix.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.Ticket

class TicketAdapter(private var tickets: List<Ticket>) :
    ListAdapter<Ticket, TicketAdapter.TicketViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textStore: TextView = itemView.findViewById(R.id.textStore)
        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        private val textAmount: TextView = itemView.findViewById(R.id.textAmount)
        private val textCategory: TextView = itemView.findViewById(R.id.textCategory)

        fun bind(ticket: Ticket) {
            textStore.text = ticket.store
            textDate.text = ticket.date
            textAmount.text = "${ticket.amount} €"
            textCategory.text = ticket.category
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Ticket>() {
        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket) = oldItem == newItem
    }
}
