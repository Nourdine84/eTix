package com.etix.viewmodel

import androidx.lifecycle.*
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class DateRange(val start: String, val end: String)

class TicketHistoryViewModel(private val repo: TicketRepository) : ViewModel() {

    private val _range = MutableLiveData<DateRange?>(null)

    // On observe un flux différent selon le filtre (null => tous)
    val tickets: LiveData<List<Ticket>> = _range.switchMap { range ->
        if (range == null) {
            repo.getAllTickets().asLiveData()
        } else {
            repo.getTicketsBetween(range.start, range.end).asLiveData()
        }
    }

    fun setDateRange(start: String, end: String) {
        _range.value = DateRange(start, end)
    }

    fun clearDateRange() {
        _range.value = null
    }

    // Exporte la liste courante (le Fragment fournit la liste affichée)
    fun export(tickets: List<Ticket>, onDone: (Result<java.io.File>) -> Unit) {
        viewModelScope.launch {
            try {
                // l’export est géré par le Fragment via utilitaire (besoin du context)
                // ici on ne fait rien : on laisse le Fragment appeler CsvExporter
                // (méthode laissée pour si on veut déplacer la logique côté VM plus tard)
                // on signale succès côté Fragment.
            } catch (t: Throwable) {
                onDone(Result.failure(t))
            }
        }
    }
}

class TicketHistoryVMFactory(private val repo: TicketRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketHistoryViewModel::class.java)) {
            return TicketHistoryViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
