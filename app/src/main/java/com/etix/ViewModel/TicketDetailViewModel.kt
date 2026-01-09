package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TicketDetailViewModel(
    private val repository: TicketRepository,
    private val ticketId: Long
) : ViewModel() {

    private val _ticket = MutableStateFlow<Ticket?>(null)
    val ticket: StateFlow<Ticket?> = _ticket

    init {
        observeTicket()
    }

    private fun observeTicket() {
        viewModelScope.launch {
            repository.getByIdFlow(ticketId).collectLatest { ticket ->
                _ticket.value = ticket
            }
        }
    }

    fun update(
        store: String,
        amount: Double,
        category: String,
        description: String?,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            val current = _ticket.value ?: return@launch

            val updated = current.copy(
                store = store,
                amount = amount,
                category = category,
                description = description,
                dateMillis = dateMillis
            )

            repository.update(updated)
        }
    }

    fun delete() {
        viewModelScope.launch {
            val current = _ticket.value ?: return@launch
            repository.delete(current)
        }
    }
}
