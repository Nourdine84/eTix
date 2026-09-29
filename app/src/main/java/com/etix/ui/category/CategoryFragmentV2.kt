package com.etix.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentCategoryV2Binding
import com.etix.features.category.CategoryStats
import com.etix.features.store.TimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Onglet Catégories (lot 5) — référence iOS CategoryView (feature/home-hero-v2).
 * Lecture seule : aucune catégorie ni aucun ticket n'est modifié. Regroupement par nom exact (comme iOS).
 * Lot 6 : toucher une ligne ouvre le détail de la catégorie. Non porté : budgets mensuels, export (écarts documentés dans docs/SUIVI_ANDROID.md).
 * L'ancien écran V1 (fragments/CategoryFragment) est conservé dans le code, non branché.
 */
class CategoryFragmentV2 : Fragment() {

    private var _binding: FragmentCategoryV2Binding? = null
    private val binding get() = _binding!!
    private val range = MutableStateFlow(TimeRange.DEFAULT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(KEY_RANGE)
            ?.let { runCatching { TimeRange.valueOf(it) }.getOrNull() }
            ?.let { range.value = it }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = CategoryV2Adapter { c ->
            (activity as? com.etix.ui.main.MainActivityV2)?.openCategoryDetail(c.name, range.value)
        }
        binding.recyclerViewCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCategories.adapter = adapter

        binding.togglePeriodCategory.check(buttonFor(range.value))
        binding.togglePeriodCategory.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            range.value = when (checkedId) {
                R.id.btnCatToday -> TimeRange.TODAY
                R.id.btnCatYear -> TimeRange.YEAR
                else -> TimeRange.MONTH
            }
        }

        val repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        val breakdown = combine(repository.getAllFlow(), range) { tickets, r ->
            CategoryStats.breakdown(tickets, r)
        }.flowOn(Dispatchers.Default)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                breakdown.collect { b ->
                    adapter.submit(b)
                    binding.emptyCategories.visibility = if (b.isEmpty) View.VISIBLE else View.GONE
                    binding.recyclerViewCategories.visibility = if (b.isEmpty) View.GONE else View.VISIBLE
                }
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
        TimeRange.TODAY -> R.id.btnCatToday
        TimeRange.MONTH -> R.id.btnCatMonth
        TimeRange.YEAR -> R.id.btnCatYear
    }

    companion object {
        private const val KEY_RANGE = "category_range"
    }
}
