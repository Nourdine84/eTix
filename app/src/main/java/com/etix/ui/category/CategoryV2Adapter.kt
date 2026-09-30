package com.etix.ui.category

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.databinding.ItemCategoryBudgetTeaserBinding
import com.etix.databinding.ItemCategoryChartBinding
import com.etix.databinding.ItemCategoryLegendBinding
import com.etix.databinding.ItemCategoryRowV2Binding
import com.etix.features.budget.BudgetLine
import com.etix.features.budget.BudgetRules
import com.etix.features.budget.BudgetStatus
import com.etix.features.budget.RowBudget
import com.etix.features.category.CategoryBreakdown
import com.etix.features.category.CategoryStats
import com.etix.features.category.CategoryTotal
import java.util.Locale

/**
 * Position 0 : carte anneau + légende ; ensuite une ligne par catégorie (ordre = total décroissant) ;
 * lot 7 : barre de budget sur « Ce mois », et invitation « Définir des budgets mensuels » en dernier tant
 * qu'aucun budget n'existe (iOS CategoryView).
 */
class CategoryV2Adapter(
    private val onCategoryClick: (CategoryTotal) -> Unit = {},
    private val onTeaserClick: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data = CategoryBreakdown(emptyList(), 0.0)
    private var budgets: Map<String, Double> = emptyMap()
    private var isMonth = true
    private var rowBudgets: Map<String, RowBudget> = emptyMap()

    fun submit(b: CategoryBreakdown, budgets: Map<String, Double> = emptyMap(), isMonth: Boolean = true) {
        data = b
        this.budgets = budgets
        this.isMonth = isMonth
        this.rowBudgets = BudgetRules.rowBudgets(b.categories.map { it.name to it.total }, budgets, isMonth)
        notifyDataSetChanged()
    }

    private val showTeaser get() = budgets.isEmpty()

    override fun getItemCount() = if (data.isEmpty) 0 else data.categories.size + 1 + (if (showTeaser) 1 else 0)
    override fun getItemViewType(position: Int) = when {
        position == 0 -> TYPE_CHART
        position <= data.categories.size -> TYPE_ROW
        else -> TYPE_TEASER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CHART -> ChartVH(ItemCategoryChartBinding.inflate(inf, parent, false))
            TYPE_TEASER -> TeaserVH(ItemCategoryBudgetTeaserBinding.inflate(inf, parent, false)).also { vh ->
                vh.itemView.setOnClickListener { onTeaserClick() }
            }
            else -> RowVH(ItemCategoryRowV2Binding.inflate(inf, parent, false), onCategoryClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChartVH -> holder.bind(data)
            is RowVH -> {
                val c = data.categories[position - 1]
                holder.bind(c, data.percent(c), rowBudgets[c.name])
            }
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

    class RowVH(
        private val b: ItemCategoryRowV2Binding,
        private val onClick: (CategoryTotal) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: CategoryTotal, percent: Double, rb: RowBudget?) {
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
            b.root.setOnClickListener { onClick(c) } // lot 6 : détail de la catégorie (iOS NavigationLink)
            val line = rb?.line
            bindBudget(rb)
            b.root.contentDescription = buildString {
                append(name).append(", ").append(euro(c.total))
                append(", ").append(String.format(Locale.FRANCE, "%.0f pour cent", percent))
                if (delta != null) append(", ").append(String.format(Locale.FRANCE, "%+.0f pour cent vs période précédente", delta))
                if (line != null) {
                    BudgetRules.sharedLabel(rb)?.let { append(", ").append(it).append(", consommation cumulée ").append(BudgetRules.formatEuro(line.spent)) }
                    BudgetRules.statusLabel(line)?.let { append(", ").append(it.replace("%", " pour cent")) }
                    append(", ").append(BudgetRules.accessibilityText(line))
                }
            }
        }

        /** iOS budgetBar : capsule 4 pt, vert / orange / rouge, libellé d'état, « dépensé / budget ». */
        private fun bindBudget(rb: RowBudget?) {
            if (rb == null) { b.budgetBlock.visibility = View.GONE; return }
            val line = rb.line
            val ctx = b.root.context
            b.budgetBlock.visibility = View.VISIBLE
            val color = ContextCompat.getColor(ctx, when (line.status) {
                BudgetStatus.OK -> R.color.v2_positive
                BudgetStatus.WARNING -> R.color.v2_attention
                BudgetStatus.EXCEEDED -> R.color.v2_negative
            })
            b.budgetBar.setIndicatorColor(color)
            b.budgetBar.progress = (BudgetRules.progress(line) * 1000).toInt()
            val label = BudgetRules.statusLabel(line)
            b.tvBudgetStatus.text = label ?: ""
            b.tvBudgetStatus.setTextColor(color)
            b.tvBudgetAmounts.text = BudgetRules.progressLabel(line)
            // Budget partagé (catégories ne différant que par la casse) : mention explicite, consommation cumulée
            val shared = BudgetRules.sharedLabel(rb)
            b.tvBudgetShared.visibility = if (shared == null) View.GONE else View.VISIBLE
            b.tvBudgetShared.text = shared?.let { "$it · consommation cumulée" } ?: ""
        }
    }

    class TeaserVH(b: ItemCategoryBudgetTeaserBinding) : RecyclerView.ViewHolder(b.root)

    companion object {
        private const val TYPE_CHART = 0
        private const val TYPE_ROW = 1
        private const val TYPE_TEASER = 2
        fun euro(v: Double): String = String.format(Locale.FRANCE, "%.2f €", v)
    }
}
