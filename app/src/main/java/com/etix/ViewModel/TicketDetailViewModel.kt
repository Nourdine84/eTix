package com.etix.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TicketDetailViewModel(
    private val ticketId: Long,
    private val repo: TicketRepository
) : ViewModel() {

    private val _ticket = MutableStateFlow<Ticket?>(null)
    val ticket: StateFlow<Ticket?> = _ticket

    init {
        loadTicket()
    }

    private fun loadTicket() {
        viewModelScope.launch {
            val result = repo.getById(ticketId)
            _ticket.value = result
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
            val current = repo.getById(ticketId) ?: return@launch
            val updatedTicket = current.copy(
                store = store,
                amount = amount,
                category = category,
                description = description,
                dateMillis = dateMillis
            )
            repo.update(updatedTicket)
        }
    }

    fun delete() {
        viewModelScope.launch {
            val current = repo.getById(ticketId) ?: return@launch
            repo.delete(current)
        }
    }
}