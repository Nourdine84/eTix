package com.etix.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.adapter.CategoryAdapter
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.viewmodel.CategoryViewModel
import com.etix.viewmodel.factory.CategoryVMFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoryFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var totalView: TextView
    private lateinit var adapter: CategoryAdapter

    private val vm: CategoryViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        CategoryVMFactory(TicketRepository(dao))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_category, container, false)

        recycler = v.findViewById(R.id.recyclerViewCategories)
        emptyView = v.findViewById(R.id.textEmptyCategories)
        totalView = v.findViewById(R.id.textGrandTotal)

        adapter = CategoryAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        observeData()

        return v
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.categories.collectLatest { list ->
                adapter.submitData(list)

                val total = list.sumOf { it.total }
                totalView.text = String.format("Total : %.2f €", total)

                val isEmpty = list.isEmpty()
                emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        }
    }
}