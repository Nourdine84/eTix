package com.etix.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.features.history.HistoryRules
import com.etix.model.Ticket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/** Historique — recherche + filtre de dates (iOS TicketHistoryView), résultat trié et groupé. */
class TicketHistoryViewModel(
    repository: TicketRepository
) : ViewModel() {

    data class DateFilter(val startDay: Long? = null, val endDay: Long? = null) {
        val active get() = startDay != null || endDay != null
    }

    private val query = MutableStateFlow("")
    val filter = MutableStateFlow(DateFilter())

    /** Tickets visibles (recherche + dates), plus récents d'abord — aussi utilisés pour l'export CSV. */
    val tickets: StateFlow<List<Ticket>> =
        combine(repository.getAllFlow(), query, filter) { list, q, f ->
            HistoryRules.filter(list, q, f.startDay, f.endDay)
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { query.value = q }
    fun setFilter(f: DateFilter) { filter.value = f }
    val hasQuery get() = query.value.isNotBlank()
}
