package com.etix.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.adapter.TicketAdapter
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.viewmodel.TicketHistoryViewModel
import com.etix.viewmodel.TicketHistoryViewModel.SortMode
import com.etix.viewmodel.factory.TicketHistoryVMFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class TicketHistoryFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var searchView: SearchView
    private lateinit var btnThisMonth: Button
    private lateinit var btnCustomRange: Button
    private lateinit var btnClearFilter: Button

    private lateinit var adapter: TicketAdapter

    private val vm: TicketHistoryViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        TicketHistoryVMFactory(TicketRepository(dao))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val v = inflater.inflate(R.layout.fragment_ticket_history, container, false)

        recycler = v.findViewById(R.id.recyclerTickets)
        emptyView = v.findViewById(R.id.textEmpty)
        searchView = v.findViewById(R.id.searchTickets)
        btnThisMonth = v.findViewById(R.id.btnThisMonth)
        btnCustomRange = v.findViewById(R.id.btnCustomRange)
        btnClearFilter = v.findViewById(R.id.btnClearFilter)

        // ✅ NAVIGATION FRAGMENT → NAVCONTROLLER (SAFE ARGS)
        adapter = TicketAdapter { ticket ->
            try {
                val action =
                    TicketHistoryFragmentDirections.actionHistoryToDetail(ticket.id.toLong())
                findNavController().navigate(action)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Navigation vers le détail impossible",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        setupSearch()
        setupFilters()
        observeTickets()
        setupSortMenu()

        return v
    }

    // 🔍 SEARCH
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?): Boolean {
                vm.setQuery(q.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                vm.setQuery(newText.orEmpty())
                return true
            }
        })
    }

    // 📅 FILTERS
    private fun setupFilters() {
        btnThisMonth.setOnClickListener {
            val (start, end) = currentMonthRange()
            vm.setDateRange(start, end)
        }

        btnCustomRange.setOnClickListener {
            pickCustomRange { s, e -> vm.setDateRange(s, e) }
        }

        btnClearFilter.setOnClickListener {
            vm.clearDateRange()
        }
    }

    // 📋 OBSERVER
    private fun observeTickets() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.tickets.collectLatest { list ->
                    adapter.submitList(list)
                    toggleEmpty(list.isEmpty())
                }
            }
        }
    }

    // 🔽 TRI
    private fun setupSortMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_history, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.sort_date_desc   -> vm.setSort(SortMode.DATE_DESC)
                    R.id.sort_date_asc    -> vm.setSort(SortMode.DATE_ASC)
                    R.id.sort_amount_desc -> vm.setSort(SortMode.AMOUNT_DESC)
                    R.id.sort_amount_asc  -> vm.setSort(SortMode.AMOUNT_ASC)
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // 🗓️ DATES
    private fun currentMonthRange(): Pair<Long, Long> {
        val c = Calendar.getInstance()

        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis

        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        val end = c.timeInMillis

        return start to end
    }

    private fun pickCustomRange(onPicked: (Long, Long) -> Unit) {
        val cal = Calendar.getInstance()

        DatePickerDialog(requireContext(), { _, y, m, d ->
            val start = Calendar.getInstance().apply {
                set(y, m, d, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            DatePickerDialog(requireContext(), { _, y2, m2, d2 ->
                val end = Calendar.getInstance().apply {
                    set(y2, m2, d2, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

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
