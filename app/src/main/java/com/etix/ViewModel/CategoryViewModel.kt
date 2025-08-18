package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import com.etix.data.TicketRepository
import com.etix.model.CategoryTotal
import kotlinx.coroutines.flow.Flow

class CategoryViewModel(repository: TicketRepository) : ViewModel() {

    val categories: Flow<List<CategoryTotal>> = repository.getCategoryTotals()
}
