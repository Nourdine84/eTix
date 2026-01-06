package com.etix.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.etix.data.TicketRepository
import com.etix.viewmodel.TicketEditViewModel

class TicketEditVMFactory(
    private val repository: TicketRepository,
    private val ticketId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TicketEditViewModel(repository, ticketId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
