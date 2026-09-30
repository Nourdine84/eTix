package com.etix.ui.store

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
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentStoreDetailBinding
import com.etix.databinding.ItemStoreCategoryBinding
import com.etix.databinding.ItemStoreTicketBinding
import com.etix.databinding.ViewV2StatCardBinding
import com.etix.features.store.StoreDetailStats
import com.etix.features.store.StoreStats
import com.etix.ui.main.MainActivityV2
import com.etix.ui.store.StoreListAdapter.Companion.euro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Fiche magasin — référence iOS StoreDetailView.
 * Non porté dans ce lot : graphique « Historique des achats » (StoreBarChartView).
 */
class StoreDetailFragment : Fragment() {

    private var _binding: FragmentStoreDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var storeKey: String
    private var showAll = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storeKey = requireArguments().getString(ARG_STORE_KEY).orEmpty()
        showAll = savedInstanceState?.getBoolean(KEY_SHOW_ALL) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStoreDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnStoreBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        val stats = repository.getAllFlow()
            .map { StoreStats.detail(it, storeKey) }
            .flowOn(Dispatchers.Default)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stats.collect { s ->
                    if (s == null) {
                        // Plus aucun ticket pour ce magasin (tous supprimés) : on referme la fiche
                        (activity as? MainActivityV2)?.closeStoreDetail()
                    } else {
                        render(s)
                    }
                }
            }
        }
    }

    private fun render(s: StoreDetailStats) {
        val ctx = requireContext()
        binding.tvDetailStoreName.text = s.storeName
        binding.tvDetailTotal.text = euro(s.total)
        binding.tvDetailCount.text = if (s.ticketCount > 1)
            "${s.ticketCount} tickets enregistrés" else "${s.ticketCount} ticket enregistré"

        stat(binding.statAverage, "PANIER MOYEN", euro(s.averageBasket))
        stat(binding.statCount, "TICKETS", s.ticketCount.toString())
        stat(binding.statLastVisit, "DERNIÈRE VISITE", s.lastVisitMillis?.let { mediumDate(it) } ?: "—")
        stat(binding.statFrequency, "FRÉQUENCE", s.avgDaysBetweenVisits?.let { "tous les $it j" } ?: "—")

        // Comparaison mensuelle : affichée seulement s'il y a des achats sur l'un des deux mois (iOS)
        val showComparison = s.thisMonthTotal > 0 || s.lastMonthTotal > 0
        binding.cardMonthComparison.visibility = if (showComparison) View.VISIBLE else View.GONE
        binding.tvThisMonth.text = euro(s.thisMonthTotal)
        binding.tvLastMonth.text = euro(s.lastMonthTotal)
        if (s.lastMonthTotal > 0) {
            binding.tvVariation.visibility = View.VISIBLE
            binding.tvVariation.text = String.format(Locale.FRANCE, "%+.1f%%", s.variationPercent)
            // iOS : hausse de dépense = rouge, baisse = vert
            binding.tvVariation.setTextColor(
                ContextCompat.getColor(ctx, if (s.variationPercent >= 0) R.color.v2_negative else R.color.v2_positive)
            )
        } else {
            binding.tvVariation.visibility = View.GONE
        }

        binding.cardTopCategories.visibility = if (s.topCategories.isEmpty()) View.GONE else View.VISIBLE
        binding.containerTopCategories.removeAllViews()
        s.topCategories.forEach { c ->
            val row = ItemStoreCategoryBinding.inflate(layoutInflater, binding.containerTopCategories, true)
            row.tvCatName.text = c.name
            row.progressCat.progress = c.percent.roundToInt().coerceIn(0, 100)
            row.tvCatPercent.text = String.format(Locale.FRANCE, "%.0f%%", c.percent)
            row.tvCatTotal.text = String.format(Locale.FRANCE, "%.0f €", c.total)
        }

        binding.tvTicketsHeader.text = "TICKETS (${s.ticketCount})"
        binding.containerStoreTickets.removeAllViews()
        val shown = if (showAll) s.tickets else s.tickets.take(PREVIEW_COUNT)
        shown.forEachIndexed { i, t ->
            if (i > 0) {
                View(ctx).apply {
                    setBackgroundColor(ContextCompat.getColor(ctx, R.color.v2_divider))
                    binding.containerStoreTickets.addView(this, ViewGroup.LayoutParams.MATCH_PARENT, 1)
                }
            }
            val row = ItemStoreTicketBinding.inflate(layoutInflater, binding.containerStoreTickets, true)
            row.tvTicketCategory.text = t.category.ifBlank { "Sans catégorie" }
            row.tvTicketDate.text = shortDate(t.dateMillis)
            row.tvTicketAmount.text = euro(t.amount)
            row.root.setOnClickListener { (activity as? MainActivityV2)?.openTicketDetail(t.id) }
        }
        val hidden = s.ticketCount - PREVIEW_COUNT
        binding.btnShowAllTickets.visibility = if (!showAll && hidden > 0) View.VISIBLE else View.GONE
        binding.btnShowAllTickets.text = "Voir les $hidden autres tickets →"
        binding.btnShowAllTickets.setOnClickListener {
            showAll = true
            render(s)
        }
    }

    private fun stat(card: ViewV2StatCardBinding, label: String, value: String) {
        card.tvStatLabel.text = label
        card.tvStatValue.text = value
    }

    private fun mediumDate(ms: Long) = SimpleDateFormat("d MMM yyyy", Locale.FRANCE).format(Date(ms))
    private fun shortDate(ms: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(ms))

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SHOW_ALL, showAll)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_STORE_KEY = "store_key"
        private const val KEY_SHOW_ALL = "show_all"
        private const val PREVIEW_COUNT = 5

        fun newInstance(storeKey: String) = StoreDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_STORE_KEY, storeKey) }
        }
    }
}
