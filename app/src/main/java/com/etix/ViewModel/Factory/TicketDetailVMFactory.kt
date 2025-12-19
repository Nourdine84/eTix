package com.etix.ViewModel.Factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.etix.ViewModel.TicketDetailViewModel
import com.etix.data.TicketRepository

class TicketDetailVMFactory(
    private val ticketId: Long,
    private val repository: TicketRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TicketDetailViewModel(ticketId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}