package com.etix.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.CategoryTotal

class CategoryAdapter(private var categories: List<CategoryTotal>) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categories[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<CategoryTotal>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryText: TextView = itemView.findViewById(R.id.textCategory)
        private val amountText: TextView = itemView.findViewById(R.id.textAmount)

        fun bind(item: CategoryTotal) {
            categoryText.text = item.category
            amountText.text = "${item.total} €"

        }
    }
}
