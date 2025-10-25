package com.etix.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.CategoryTotal

class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var categories: List<CategoryTotal> = emptyList()
    private var grandTotal: Double = 0.0

    fun submitData(list: List<CategoryTotal>) {
        categories = list
        grandTotal = list.sumOf { it.total }
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvCatName)
        val tvAmount: TextView = v.findViewById(R.id.tvCatAmount)
        val tvPercent: TextView = v.findViewById(R.id.tvPercent)
        val progress: ProgressBar = v.findViewById(R.id.progressPercent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_total, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        val percent = if (grandTotal > 0) (item.total / grandTotal * 100).toInt() else 0

        holder.tvName.text = item.name
        holder.tvAmount.text = String.format("%.2f €", item.total)
        holder.tvPercent.text = "$percent %"
        holder.progress.progress = percent
    }

    override fun getItemCount() = categories.size
}