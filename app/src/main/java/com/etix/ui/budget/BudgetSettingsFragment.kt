package com.etix.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.BudgetStore
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentBudgetSettingsBinding
import com.etix.databinding.ItemBudgetSettingBinding
import com.etix.features.budget.BudgetRules
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Budgets mensuels (lot 7) — iOS BudgetSettingsView. Catégories = catégories DISTINCTES des tickets (lecture seule
 * sur les tickets) ; toucher une ligne ouvre la saisie. Stockage : BudgetStore (préférences dédiées, additif).
 */
class BudgetSettingsFragment : Fragment() {

    private var _binding: FragmentBudgetSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBudgetsBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        val store = BudgetStore(requireContext())
        val repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        val rows = combine(repository.distinctCategoriesFlow(), BudgetStore.version) { cats, _ ->
            BudgetRules.settingsCategories(cats) to store.load()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                rows.collect { (cats, budgets) -> bind(cats, budgets, store) }
            }
        }
    }

    private fun bind(categories: List<String>, budgets: Map<String, Double>, store: BudgetStore) {
        val container = binding.budgetRows
        container.removeAllViews()
        binding.cardBudgetList.visibility = if (categories.isEmpty()) View.GONE else View.VISIBLE
        binding.tvBudgetsEmpty.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
        val inf = LayoutInflater.from(requireContext())
        categories.forEachIndexed { i, cat ->
            if (i > 0) container.addView(View(requireContext()).apply {
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.v2_divider))
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    .apply { marginStart = (16 * resources.displayMetrics.density).toInt() }
            })
            val row = ItemBudgetSettingBinding.inflate(inf, container, true)
            val limit = budgets[BudgetRules.key(cat)]
            row.tvBudgetCategory.text = cat
            row.tvBudgetValue.text = limit?.let { BudgetRules.formatEuro(it) } ?: "—"
            row.root.contentDescription = "$cat, " + (limit?.let { "budget ${BudgetRules.formatEuro(it)}" } ?: "aucun budget")
            row.root.setOnClickListener { (activity as? com.etix.ui.main.MainActivityV2)?.openBudgetEdit(cat) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
