package com.etix.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.etix.data.TicketRepository
import com.etix.viewmodel.AddTicketViewModel

class AddTicketVMFactory(private val repository: TicketRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddTicketViewModel(repository) as T
    }
}
