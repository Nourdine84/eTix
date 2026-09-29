package com.etix.ui.category

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.databinding.ItemCategoryChartBinding
import com.etix.databinding.ItemCategoryLegendBinding
import com.etix.databinding.ItemCategoryRowV2Binding
import com.etix.features.category.CategoryBreakdown
import com.etix.features.category.CategoryStats
import com.etix.features.category.CategoryTotal
import java.util.Locale

/** Position 0 : carte anneau + légende ; ensuite une ligne par catégorie (ordre = total décroissant). */
class CategoryV2Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data = CategoryBreakdown(emptyList(), 0.0)

    fun submit(b: CategoryBreakdown) {
        data = b
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (data.isEmpty) 0 else data.categories.size + 1
    override fun getItemViewType(position: Int) = if (position == 0) TYPE_CHART else TYPE_ROW

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_CHART) ChartVH(ItemCategoryChartBinding.inflate(inf, parent, false))
        else RowVH(ItemCategoryRowV2Binding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChartVH -> holder.bind(data)
            is RowVH -> holder.bind(data.categories[position - 1], data.percent(data.categories[position - 1]))
        }
    }

    class ChartVH(private val b: ItemCategoryChartBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(d: CategoryBreakdown) {
            val ctx = b.root.context
            b.donut.setSlices(d.categories.mapIndexed { i, c -> c.total to CategoryDonutView.colorAt(ctx, i) })
            b.tvDonutTotal.text = euro(d.grandTotal)
            b.legendContainer.removeAllViews()
            val inf = LayoutInflater.from(ctx)
            d.categories.forEachIndexed { i, c ->
                val l = ItemCategoryLegendBinding.inflate(inf, b.legendContainer, true)
                l.legendDot.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(CategoryDonutView.colorAt(ctx, i))
                }
                l.legendName.text = CategoryStats.displayName(c.name)
                l.legendPercent.text = String.format(Locale.FRANCE, "%.1f %%", d.percent(c))
            }
            b.donutFrame.contentDescription = "Répartition par catégorie, total ${euro(d.grandTotal)}"
        }
    }

    class RowVH(private val b: ItemCategoryRowV2Binding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: CategoryTotal, percent: Double) {
            val ctx = b.root.context
            val name = CategoryStats.displayName(c.name)
            b.tvCategoryName.text = name
            b.tvCategoryTotal.text = euro(c.total)
            val delta = c.deltaPercent
            if (delta == null) {
                b.tvCategoryDelta.text = ""
            } else {
                b.tvCategoryDelta.text = String.format(Locale.FRANCE, "%+.0f%%", delta)
                // iOS : hausse des dépenses en rouge, baisse en vert
                b.tvCategoryDelta.setTextColor(ContextCompat.getColor(ctx,
                    if (delta >= 0) R.color.v2_negative else R.color.v2_positive))
            }
            b.tvCategoryPercent.text = String.format(Locale.FRANCE, "%.0f %%", percent)
            b.root.contentDescription = buildString {
                append(name).append(", ").append(euro(c.total))
                append(", ").append(String.format(Locale.FRANCE, "%.0f pour cent", percent))
                if (delta != null) append(", ").append(String.format(Locale.FRANCE, "%+.0f pour cent vs période précédente", delta))
            }
        }
    }

    companion object {
        private const val TYPE_CHART = 0
        private const val TYPE_ROW = 1
        fun euro(v: Double): String = String.format(Locale.FRANCE, "%.2f €", v)
    }
}
