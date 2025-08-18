package com.etix.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import com.etix.util.CsvExporter
import com.etix.util.DateUtils
import com.etix.viewmodel.TicketHistoryVMFactory
import com.etix.viewmodel.TicketHistoryViewModel
import java.util.Calendar

class TicketHistoryFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TicketAdapter
    private lateinit var btnThisMonth: Button
    private lateinit var btnCustomRange: Button
    private lateinit var btnClearFilter: Button
    private lateinit var btnExportCsv: Button

    private var currentList: List<Ticket> = emptyList()

    private val vm: TicketHistoryViewModel by viewModels {
        val dao = AppDatabase.getDatabase(requireContext()).ticketDao()
        TicketHistoryVMFactory(TicketRepository(dao))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_ticket_history, container, false)

        recycler = v.findViewById(R.id.recyclerTickets)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TicketAdapter(emptyList())
        recycler.adapter = adapter

        btnThisMonth   = v.findViewById(R.id.btnThisMonth)
        btnCustomRange = v.findViewById(R.id.btnCustomRange)
        btnClearFilter = v.findViewById(R.id.btnClearFilter)
        btnExportCsv   = v.findViewById(R.id.btnExportCsv)

        // Observe la liste depuis le ViewModel
        vm.tickets.observe(viewLifecycleOwner) { list ->
            currentList = list
            adapter.updateData(list)
        }

        // Filtres rapides
        btnThisMonth.setOnClickListener {
            val start = DateUtils.firstDayOfCurrentMonth()
            val end   = DateUtils.lastDayOfCurrentMonth()
            vm.setDateRange(start, end)
            Toast.makeText(requireContext(), "Filtre: $start → $end", Toast.LENGTH_SHORT).show()
        }

        btnClearFilter.setOnClickListener {
            vm.clearDateRange()
            Toast.makeText(requireContext(), "Filtre désactivé", Toast.LENGTH_SHORT).show()
        }

        // Période personnalisée (deux DatePickers)
        btnCustomRange.setOnClickListener {
            pickCustomRange { start, end ->
                vm.setDateRange(start, end)
                Toast.makeText(requireContext(), "Filtre: $start → $end", Toast.LENGTH_SHORT).show()
            }
        }

        // Export CSV
        btnExportCsv.setOnClickListener {
            if (currentList.isEmpty()) {
                Toast.makeText(requireContext(), "Aucun ticket à exporter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val file = CsvExporter.exportTickets(requireContext(), currentList)
            Toast.makeText(requireContext(), "CSV exporté: ${file.name}", Toast.LENGTH_LONG).show()
        }

        return v
    }

    private fun pickCustomRange(onPicked: (String, String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val start = "${y}-${(m + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
                // deuxième picker pour la date de fin
                DatePickerDialog(
                    requireContext(),
                    { _, y2, m2, d2 ->
                        val end = "${y2}-${(m2 + 1).toString().padStart(2, '0')}-${d2.toString().padStart(2, '0')}"
                        onPicked(start, end)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
