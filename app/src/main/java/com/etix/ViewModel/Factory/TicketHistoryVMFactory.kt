package com.etix.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.etix.data.TicketRepository
import com.etix.viewmodel.TicketHistoryViewModel

class TicketHistoryVMFactory(
    private val repository: TicketRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TicketHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
