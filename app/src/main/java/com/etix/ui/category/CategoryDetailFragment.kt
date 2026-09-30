package com.etix.ui.category

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentCategoryDetailBinding
import com.etix.databinding.ItemCategoryTicketBinding
import com.etix.features.category.CategoryDetail
import com.etix.features.category.CategoryStats
import com.etix.features.store.TimeRange
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Détail d'une catégorie (lot 6) — référence iOS CategoryDetailView. Lecture seule : tickets de la catégorie EXACTE
 * sur la période ; toucher un ticket ouvre son détail (Modifier / Supprimer avec confirmation y restent).
 */
class CategoryDetailFragment : Fragment() {

    private var _binding: FragmentCategoryDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var categoryName: String
    private val range = MutableStateFlow(TimeRange.DEFAULT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryName = requireArguments().getString(ARG_NAME).orEmpty()
        val initial = savedInstanceState?.getString(KEY_RANGE) ?: requireArguments().getString(ARG_RANGE)
        initial?.let { runCatching { TimeRange.valueOf(it) }.getOrNull() }?.let { range.value = it }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val display = CategoryStats.displayName(categoryName)
        binding.tvCategoryDetailTitle.text = display
        binding.tvCategoryDetailName.text = display
        binding.btnCategoryBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        binding.togglePeriodCategoryDetail.check(buttonFor(range.value))
        binding.togglePeriodCategoryDetail.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            range.value = when (id) {
                R.id.btnCatDetailToday -> TimeRange.TODAY
                R.id.btnCatDetailYear -> TimeRange.YEAR
                else -> TimeRange.MONTH
            }
        }

        val repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        val detail = combine(repository.getAllFlow(), range) { all, r -> CategoryStats.detail(all, categoryName, r) }
            .flowOn(Dispatchers.Default)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detail.collect(::bind)
            }
        }
    }

    private fun bind(d: CategoryDetail) {
        val total = String.format(Locale.FRANCE, "%.2f €", d.total)
        binding.tvCategoryDetailTotal.text = total
        binding.tvCategoryDetailCount.text = "${d.count} ticket${if (d.count > 1) "s" else ""}"

        binding.cardDayChart.visibility = if (d.days.isEmpty()) View.GONE else View.VISIBLE
        binding.dayChart.setData(d.chart)
        binding.dayChartScroll.post { _binding?.dayChartScroll?.fullScroll(View.FOCUS_RIGHT) } // jours récents visibles

        binding.emptyCategoryDetail.visibility = if (d.days.isEmpty()) View.VISIBLE else View.GONE
        val container = binding.daySections
        container.removeAllViews()
        val ctx = requireContext()
        val inf = LayoutInflater.from(ctx)
        val dateFmt = DateFormat.getDateInstance(DateFormat.MEDIUM)
        for (section in d.days) {
            container.addView(TextView(ctx).apply {
                text = CategoryStats.sectionTitle(section.dayStart)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.v2_text_secondary))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = (20 * resources.displayMetrics.density).toInt() }
                if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            })
            for (t in section.tickets) {
                val row = ItemCategoryTicketBinding.inflate(inf, container, true)
                val amount = String.format(Locale.FRANCE, "%.2f €", t.amount)
                row.tvCatTicketStore.text = t.store
                row.tvCatTicketAmount.text = amount
                row.tvCatTicketDate.text = dateFmt.format(Date(t.dateMillis))
                row.root.contentDescription = "${t.store}, $amount"
                row.root.setOnClickListener { (activity as? MainActivityV2)?.openTicketDetail(t.id) }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_RANGE, range.value.name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buttonFor(r: TimeRange) = when (r) {
        TimeRange.TODAY -> R.id.btnCatDetailToday
        TimeRange.MONTH -> R.id.btnCatDetailMonth
        TimeRange.YEAR -> R.id.btnCatDetailYear
    }

    companion object {
        private const val ARG_NAME = "category_name"
        private const val ARG_RANGE = "category_range"
        private const val KEY_RANGE = "category_detail_range"

        /** iOS : NavigationLink(CategoryDetailView(categoryName:, initialRange: range)). */
        fun newInstance(name: String, range: TimeRange) = CategoryDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_NAME, name); putString(ARG_RANGE, range.name) }
        }
    }
}
