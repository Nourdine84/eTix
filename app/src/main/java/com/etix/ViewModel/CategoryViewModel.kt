package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.CategoryTotal
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryViewModel(private val repo: TicketRepository) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryTotal>>(emptyList())
    val categories: StateFlow<List<CategoryTotal>> = _categories.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getCategoryTotals()
                .collectLatest { _categories.value = it }
        }
    }
}