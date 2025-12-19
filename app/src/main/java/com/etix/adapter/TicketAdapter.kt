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
import java.text.SimpleDateFormat
import java.util.*

class TicketAdapter(
    private val onItemClick: ((Ticket) -> Unit)? = null
) : ListAdapter<Ticket, TicketAdapter.VH>(DiffCb) {

    object DiffCb : DiffUtil.ItemCallback<Ticket>() {
        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket) = oldItem == newItem
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvStore: TextView = v.findViewById(R.id.tvStore)
        val tvAmount: TextView = v.findViewById(R.id.tvAmount)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvCategory: TextView = v.findViewById(R.id.tvCategory)
    }

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ticket, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = getItem(position)
        holder.tvStore.text = t.store
        holder.tvAmount.text = String.format(Locale.getDefault(), "%.2f €", t.amount)
        holder.tvDate.text = df.format(Date(t.dateMillis))
        holder.tvCategory.text = t.category

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(t)
        }
    }
}