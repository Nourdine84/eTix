package com.etix.ui.store

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.databinding.ItemStoreCardBinding
import com.etix.features.store.StoreStats
import com.etix.features.store.StoreTotal
import java.util.Locale

class StoreListAdapter(
    private val onStoreClick: (StoreTotal) -> Unit
) : RecyclerView.Adapter<StoreListAdapter.VH>() {

    private var items: List<StoreTotal> = emptyList()
    private var grandTotal = 0.0

    fun submit(list: List<StoreTotal>) {
        items = list
        grandTotal = list.sumOf { it.total }
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemStoreCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position + 1)

    inner class VH(private val b: ItemStoreCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(store: StoreTotal, rank: Int) {
            val ctx = b.root.context
            // iOS : rang affiché pour le top 3, N°1 en bleu
            if (rank <= 3) {
                b.tvRank.visibility = View.VISIBLE
                b.tvRank.text = "N°$rank"
                b.tvRank.setTextColor(
                    ContextCompat.getColor(ctx, if (rank == 1) R.color.v2_primary else R.color.v2_text_secondary)
                )
            } else {
                b.tvRank.visibility = View.GONE
            }
            b.tvStoreName.text = store.storeName
            b.tvStoreTotal.text = euro(store.total)
            b.tvStoreMeta.text = String.format(
                Locale.FRANCE, "%d ticket(s) · %.2f €/visite · %.0f%%",
                store.ticketCount, store.averageBasket, StoreStats.sharePercent(store, grandTotal)
            )
            b.tvStoreLastVisit.text = StoreStats.relativeLastVisit(store.lastPurchaseMillis)
            b.root.contentDescription =
                "${store.storeName}, ${euro(store.total)}, ${store.ticketCount} ticket(s)"
            b.root.setOnClickListener { onStoreClick(store) }
        }
    }

    companion object {
        fun euro(v: Double): String = String.format(Locale.FRANCE, "%.2f €", v)
    }
}
