package com.etix.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.DialogHistoryFilterBinding
import com.etix.databinding.FragmentTicketHistoryV2Binding
import com.etix.features.history.HistoryRules
import com.etix.features.ticket.TicketFormRules
import com.etix.ui.main.MainActivityV2
import com.etix.utils.CsvExporter
import com.etix.viewmodel.factory.TicketHistoryVMFactory
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * Historique — référence iOS TicketHistoryView : recherche (magasin / catégorie), filtre de dates,
 * sections par période, tri du plus récent au plus ancien, états vides. Export CSV Android conservé.
 */
class TicketHistoryFragmentV2 : Fragment() {

    private var _binding: FragmentTicketHistoryV2Binding? = null
    private val binding get() = _binding!!

    private val viewModel: TicketHistoryViewModel by viewModels {
        TicketHistoryVMFactory(TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao()))
    }
    private lateinit var adapter: TicketHistoryAdapter
    private val dayFormat get() = DateFormat.getDateInstance(DateFormat.MEDIUM)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketHistoryV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = TicketHistoryAdapter { ticket -> (activity as? MainActivityV2)?.openTicketDetail(ticket.id) }
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tickets.collect { list ->
                    adapter.submitSections(HistoryRules.group(list))
                    renderEmpty(list.isEmpty())
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filter.collect { renderFilter(it) }
            }
        }

        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { viewModel.setQuery(s?.toString().orEmpty()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        binding.btnFilter.setOnClickListener { openFilter() }
        binding.btnClearFilters.setOnClickListener { viewModel.setFilter(TicketHistoryViewModel.DateFilter()) }
        binding.tvFilterSummary.setOnClickListener { openFilter() }
        binding.btnExportCsv.setOnClickListener { exportCsv() }
    }

    private fun renderEmpty(empty: Boolean) {
        val filtered = viewModel.filter.value.active
        binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerHistory.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvEmptyTitle.text = if (!viewModel.hasQuery && !filtered) "Aucun ticket" else "Aucun résultat"
        binding.btnClearFilters.visibility = if (empty && filtered) View.VISIBLE else View.GONE
    }

    /** iOS : icône de filtre pleine/bleue quand un filtre est actif. Android : résumé textuel en plus. */
    private fun renderFilter(f: TicketHistoryViewModel.DateFilter) {
        val ctx = requireContext()
        binding.btnFilter.imageTintList = ContextCompat.getColorStateList(ctx,
            if (f.active) R.color.v2_primary else R.color.v2_text_primary)
        binding.btnFilter.contentDescription = if (f.active) "Filtrer par dates, filtre actif" else "Filtrer par dates"
        binding.tvFilterSummary.visibility = if (f.active) View.VISIBLE else View.GONE
        binding.tvFilterSummary.text = when {
            f.startDay != null && f.endDay != null -> "Du ${dayFormat.format(Date(f.startDay))} au ${dayFormat.format(Date(f.endDay))}"
            f.startDay != null -> "Depuis le ${dayFormat.format(Date(f.startDay))}"
            f.endDay != null -> "Jusqu'au ${dayFormat.format(Date(f.endDay))}"
            else -> ""
        }
        renderEmpty(viewModel.tickets.value.isEmpty())
    }

    /** iOS TicketFilterSheet : début / fin activables séparément ; fin ≥ début ; Appliquer, Fermer, Réinitialiser. */
    private fun openFilter() {
        val d = DialogHistoryFilterBinding.inflate(layoutInflater)
        val current = viewModel.filter.value
        val now = System.currentTimeMillis()
        var start: Long? = current.startDay
        var end: Long? = current.endDay
        // iOS : valeurs proposées par défaut = il y a un mois → aujourd'hui
        var tempStart = current.startDay ?: Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
        var tempEnd = current.endDay ?: now

        fun render() {
            d.switchStart.isChecked = start != null
            d.switchEnd.isChecked = end != null
            d.tvStart.visibility = if (start != null) View.VISIBLE else View.GONE
            d.tvEnd.visibility = if (end != null) View.VISIBLE else View.GONE
            start?.let { d.tvStart.text = "Début : ${dayFormat.format(Date(it))}" }
            end?.let { d.tvEnd.text = "Fin : ${dayFormat.format(Date(it))}" }
        }
        fun pick(initial: Long, min: Long?, max: Long?, onPicked: (Long) -> Unit) {
            val validators = listOfNotNull(
                min?.let { DateValidatorPointForward.from(TicketFormRules.toPickerSelection(it)) },
                max?.let { DateValidatorPointBackward.before(TicketFormRules.toPickerSelection(it) + 86_400_000L) }
            )
            val builder = MaterialDatePicker.Builder.datePicker().setSelection(TicketFormRules.toPickerSelection(initial))
            if (validators.isNotEmpty()) {
                builder.setCalendarConstraints(CalendarConstraints.Builder()
                    .setValidator(com.google.android.material.datepicker.CompositeDateValidator.allOf(validators)).build())
            }
            val picker = builder.build()
            picker.addOnPositiveButtonClickListener { sel -> onPicked(TicketFormRules.combineDay(sel, initial)) }
            picker.show(childFragmentManager, "history_filter_date")
        }

        d.switchStart.setOnCheckedChangeListener { _, on -> start = if (on) (start ?: tempStart) else null; render() }
        d.switchEnd.setOnCheckedChangeListener { _, on ->
            end = if (on) (end ?: maxOf(tempEnd, start ?: tempEnd)) else null; render()
        }
        d.tvStart.setOnClickListener { pick(start ?: tempStart, null, end) { start = it; tempStart = it; render() } }
        d.tvEnd.setOnClickListener { pick(end ?: tempEnd, start, null) { end = it; tempEnd = it; render() } }
        render()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtres")
            .setView(d.root)
            .setPositiveButton("Appliquer") { _, _ -> viewModel.setFilter(TicketHistoryViewModel.DateFilter(start, end)) }
            .setNegativeButton("Fermer", null)
            .setNeutralButton("Réinitialiser") { _, _ -> viewModel.setFilter(TicketHistoryViewModel.DateFilter()) }
            .show()
    }

    private fun exportCsv() {
        val tickets = viewModel.tickets.value
        if (tickets.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun ticket à exporter", Toast.LENGTH_SHORT).show()
            return
        }
        val file = CsvExporter.export(requireContext(), tickets)
        Toast.makeText(requireContext(), "CSV exporté : ${file.name}", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
