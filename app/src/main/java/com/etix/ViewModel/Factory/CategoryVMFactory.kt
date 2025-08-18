package com.etix.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.etix.data.TicketRepository
import com.etix.viewmodel.CategoryViewModel

class CategoryVMFactory(private val repository: TicketRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CategoryViewModel(repository) as T
    }
}
