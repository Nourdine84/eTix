package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TicketEditViewModel(
    private val repository: TicketRepository,
    private val ticketId: Long
) : ViewModel() {

    private val _ticket = MutableStateFlow<Ticket?>(null)
    val ticket: StateFlow<Ticket?> = _ticket

    init {
        viewModelScope.launch {
            _ticket.value = repository.getById(ticketId)
        }
    }

    fun updateTicket(
        store: String,
        amount: Double,
        category: String,
        description: String?,
        dateMillis: Long,
        onDone: () -> Unit
    ) {
        val current = _ticket.value ?: return

        val updated = current.copy(
            store = store,
            amount = amount,
            category = category.ifBlank { "Autre" },
            description = description?.ifBlank { null },
            dateMillis = dateMillis
        )

        viewModelScope.launch {
            repository.update(updated)
            onDone()
        }
    }
}
