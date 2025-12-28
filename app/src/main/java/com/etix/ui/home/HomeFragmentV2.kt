package com.etix.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.viewmodel.HomeViewModel
import com.etix.viewmodel.factory.HomeVMFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class HomeFragmentV2 : Fragment(R.layout.fragment_home_v2) {

    private val viewModel: HomeViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        val repo = TicketRepository(dao)
        HomeVMFactory(repo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvToday = view.findViewById<TextView>(R.id.tvTodayAmount)
        val tvMonth = view.findViewById<TextView>(R.id.tvMonthAmount)
        val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)

        lifecycleScope.launch {
            viewModel.ui.collectLatest { state ->
                tvToday.text = formatter.format(state.todayTotal)
                tvMonth.text = formatter.format(state.monthTotal)
            }
        }

        view.findViewById<Button>(R.id.btnAddTicket).setOnClickListener {
            findNavController().navigate(R.id.menu_add)
        }

        view.findViewById<Button>(R.id.btnHistory).setOnClickListener {
            findNavController().navigate(R.id.menu_history)
        }
    }
}
