package com.etix.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etix.databinding.ItemHistorySectionBinding
import com.etix.databinding.ItemTicketHistoryBinding
import com.etix.features.history.HistoryRules
import com.etix.model.Ticket
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Liste sectionnée (en-têtes de période + cartes ticket) — iOS TicketHistoryView. */
class TicketHistoryAdapter(
    private val onTicketClick: (Ticket) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class Header(val label: String) : Row()
        data class Item(val ticket: Ticket) : Row()
    }

    private var rows: List<Row> = emptyList()
    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    fun submitSections(sections: List<HistoryRules.Section>) {
        rows = sections.flatMap { s -> listOf(Row.Header(s.label)) + s.tickets.map { Row.Item(it) } }
        notifyDataSetChanged()
    }

    /** Compatibilité (appels existants) : liste simple sans sections. */
    fun submitList(list: List<Ticket>) {
        rows = list.map { Row.Item(it) }
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = if (rows[position] is Row.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == 0) HeaderVH(ItemHistorySectionBinding.inflate(inf, parent, false))
        else TicketVH(ItemTicketHistoryBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val r = rows[position]) {
            is Row.Header -> (holder as HeaderVH).b.tvSection.text = r.label
            is Row.Item -> (holder as TicketVH).bind(r.ticket)
        }
    }

    class HeaderVH(val b: ItemHistorySectionBinding) : RecyclerView.ViewHolder(b.root)

    inner class TicketVH(private val b: ItemTicketHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(t: Ticket) {
            val amount = String.format(Locale.FRANCE, "%.2f €", t.amount)
            val date = dateFormat.format(Date(t.dateMillis))
            b.tvStore.text = t.store
            b.tvDate.text = if (t.category.isNotBlank()) "${t.category} · $date" else date
            b.tvAmount.text = amount
            b.root.contentDescription = "${t.store}, $amount, ${b.tvDate.text}"
            b.root.setOnClickListener { onTicketClick(t) }
        }
    }
}
