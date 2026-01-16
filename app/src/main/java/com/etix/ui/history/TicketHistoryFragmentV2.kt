package com.etix.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentTicketHistoryV2Binding
import com.etix.utils.CsvExporter
import kotlinx.coroutines.launch

class TicketHistoryFragmentV2 : Fragment() {

    private var _binding: FragmentTicketHistoryV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TicketHistoryViewModel
    private lateinit var adapter: TicketHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketHistoryV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        val repository = TicketRepository(dao)
        viewModel = TicketHistoryViewModel(repository)

        adapter = TicketHistoryAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        lifecycleScope.launch {
            viewModel.tickets.collect { list ->
                adapter.submitList(list)
                binding.emptyState.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // 🔍 Recherche (FIXED)
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setQuery(s?.toString().orEmpty())
            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {}
        })

        // 📤 EXPORT CSV
        binding.btnExportCsv.setOnClickListener {
            exportCsv()
        }
    }

    private fun exportCsv() {
        lifecycleScope.launch {
            val tickets = viewModel.tickets.value

            if (tickets.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Aucun ticket à exporter",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val file = CsvExporter.export(requireContext(), tickets)

            Toast.makeText(
                requireContext(),
                "CSV exporté : ${file.name}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
