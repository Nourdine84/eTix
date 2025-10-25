package com.etix.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.launch

class AddTicketViewModel(private val repository: TicketRepository) : ViewModel() {
    val saving = MutableLiveData(false)
    val error = MutableLiveData<String?>()

    fun insertTicket(ticket: Ticket) {
        viewModelScope.launch {
            try {
                saving.value = true
                repository.insert(ticket)
                error.value = null
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                saving.value = false
            }
        }
    }
}
