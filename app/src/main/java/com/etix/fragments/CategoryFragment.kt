package com.etix.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etix.adapter.CategoryAdapter
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentCategoryBinding
import com.etix.viewmodel.CategoryViewModel
import com.etix.viewmodel.factory.CategoryVMFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CategoryAdapter

    private val viewModel: CategoryViewModel by viewModels {
        CategoryVMFactory(TicketRepository(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CategoryAdapter(emptyList())
        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCategories.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collectLatest { list ->
                adapter.updateData(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
