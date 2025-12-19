package com.etix.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.CategoryTotal
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter simple pour afficher les totaux par catégorie.
 * Aligne les IDs sur item_category_total.xml :
 *  - tvCatName
 *  - tvCatAmount
 *  - tvPercent
 */
class CategoryTotalsAdapter : RecyclerView.Adapter<CategoryTotalsAdapter.ViewHolder>() {

    private val currency = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private var items: List<CategoryTotal> = emptyList()
    private var grandTotal: Double = 0.0

    /** Mets à jour la liste + le total général pour calculer le pourcentage. */
    fun submit(newItems: List<CategoryTotal>, grand: Double) {
        items = newItems
        grandTotal = grand.coerceAtLeast(0.0)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvCatName)
        val amount: TextView = view.findViewById(R.id.tvCatAmount)
        val percent: TextView = view.findViewById(R.id.tvPercent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_total, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.amount.text = currency.format(item.total)

        val pct = if (grandTotal > 0.0) (item.total / grandTotal * 100.0) else 0.0
        val pctInt = pct.coerceIn(0.0, 100.0).toInt()
        holder.percent.text = "$pctInt%"
    }

    override fun getItemCount(): Int = items.size
}