package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.etix.data.TicketRepository

class AddTicketViewModelFactory(
    private val repository: TicketRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTicketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddTicketViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
