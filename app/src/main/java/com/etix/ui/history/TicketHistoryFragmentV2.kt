package com.etix.ui.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.viewmodel.TicketHistoryViewModel
import com.etix.viewmodel.factory.TicketHistoryVMFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TicketHistoryFragmentV2 : Fragment(R.layout.fragment_ticket_history) {

    private lateinit var adapter: TicketHistoryAdapter

    private val viewModel: TicketHistoryViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        val repo = TicketRepository(dao)
        TicketHistoryVMFactory(repo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerTickets)
        adapter = TicketHistoryAdapter(emptyList())

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        lifecycleScope.launch {
            viewModel.tickets.collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }
}
