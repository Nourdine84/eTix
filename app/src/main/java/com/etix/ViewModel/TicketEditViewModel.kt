package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TicketEditViewModel(
    private val repository: TicketRepository,
    private val ticketId: Long
) : ViewModel() {

    // ✅ Ticket observé en Flow/StateFlow
    val ticket: StateFlow<Ticket?> =
        repository.getByIdFlow(ticketId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    // ✅ Mise à jour (onDone OPTIONNEL → corrige "No value passed for parameter 'onDone'")
    fun updateTicket(
        store: String,
        amount: Double,
        category: String,
        description: String?,
        dateMillis: Long,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val current = ticket.value ?: return@launch

            val updated = current.copy(
                store = store,
                amount = amount,
                category = category,
                description = description,
                dateMillis = dateMillis
            )

            repository.update(updated)
            onDone()
        }
    }

    fun deleteTicket(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            ticket.value?.let {
                repository.delete(it)
                onDone()
            }
        }
    }
}
