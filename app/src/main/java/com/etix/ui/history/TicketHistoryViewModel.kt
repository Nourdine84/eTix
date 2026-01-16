package com.etix.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.flow.*

class TicketHistoryViewModel(
    private val repository: TicketRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val ticketsFlow = repository.getAllFlow()

    val tickets: StateFlow<List<Ticket>> =
        combine(ticketsFlow, query) { list, q ->
            if (q.isBlank()) list
            else list.filter {
                it.store.contains(q, true) ||
                        it.category.contains(q, true) ||
                        (it.description?.contains(q, true) ?: false)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun setQuery(q: String) {
        query.value = q
    }
}
