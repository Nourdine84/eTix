package com.etix.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.etix.data.TicketRepository
import com.etix.viewmodel.TicketEditViewModel

class TicketEditVMFactory(
    private val repository: TicketRepository,
    private val ticketId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketEditViewModel::class.java)) {
            return TicketEditViewModel(repository, ticketId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
