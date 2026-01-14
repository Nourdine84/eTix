package com.etix.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.launch

class AddTicketViewModel(
    private val repository: TicketRepository
) : ViewModel() {

    fun saveTicket(ticket: Ticket) {
        viewModelScope.launch {
            repository.insert(ticket)
        }
    }
}
