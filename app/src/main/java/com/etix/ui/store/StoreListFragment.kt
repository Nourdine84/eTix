package com.etix.ui.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.etix.features.settings.AppPreferences
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentStoreListBinding
import com.etix.features.store.StoreStats
import com.etix.features.store.TimeRange
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Onglet Magasins — référence iOS StoreListView (feature/home-hero-v2).
 * Non porté dans ce lot : bouton « Comparaison » (StoreComparisonView).
 */
class StoreListFragment : Fragment() {

    private var _binding: FragmentStoreListBinding? = null
    private val binding get() = _binding!!

    private val range = MutableStateFlow(TimeRange.DEFAULT)
    private var appliedDefault: TimeRange = TimeRange.DEFAULT
    private var defaultRangeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lot 10 : période par défaut des Réglages à l'ouverture ; un choix fait sur l'écran est conservé ensuite
        val prefs = AppPreferences(requireContext())
        val parse = { k: String -> savedInstanceState?.getString(k)?.let { runCatching { TimeRange.valueOf(it) }.getOrNull() } }
        appliedDefault = prefs.defaultRange
        range.value = AppPreferences.initialRange(parse(KEY_RANGE), parse(KEY_APPLIED_DEFAULT), appliedDefault)
        // Seul un changement du réglage remplace la période affichée ; changer d'onglet ne la réinitialise pas
        defaultRangeListener = prefs.listenDefaultRange { d ->
            if (d != appliedDefault) {
                appliedDefault = d
                range.value = d
                _binding?.togglePeriod?.check(buttonFor(d))
            }
        }
    }

    override fun onDestroy() {
        defaultRangeListener?.let { AppPreferences(requireContext()).stopListening(it) }
        defaultRangeListener = null
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStoreListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = StoreListAdapter { store ->
            (activity as? MainActivityV2)?.openStoreDetail(store.key)
        }
        binding.recyclerStores.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerStores.adapter = adapter

        binding.togglePeriod.check(buttonFor(range.value))
        binding.togglePeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            range.value = when (checkedId) {
                R.id.btnPeriodToday -> TimeRange.TODAY
                R.id.btnPeriodYear -> TimeRange.YEAR
                else -> TimeRange.MONTH
            }
        }

        val repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        val stores = combine(repository.getAllFlow(), range) { tickets, r ->
            StoreStats.storeTotals(tickets, r.currentRange())
        }.flowOn(Dispatchers.Default)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stores.collect { list ->
                    adapter.submit(list)
                    binding.emptyStores.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerStores.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_RANGE, range.value.name)
        outState.putString(KEY_APPLIED_DEFAULT, appliedDefault.name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buttonFor(r: TimeRange) = when (r) {
        TimeRange.TODAY -> R.id.btnPeriodToday
        TimeRange.MONTH -> R.id.btnPeriodMonth
        TimeRange.YEAR -> R.id.btnPeriodYear
    }

    companion object {
        private const val KEY_RANGE = "store_range"
        private const val KEY_APPLIED_DEFAULT = "store_applied_default"
    }
}
