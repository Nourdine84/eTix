package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TicketHistoryViewModel(
    private val repository: TicketRepository
) : ViewModel() {

    enum class SortMode { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets

    // État des filtres
    private var range: Pair<Long, Long>? = null
    private var query: String = ""
    private var sortMode: SortMode = SortMode.DATE_DESC

    private var loadJob: Job? = null

    /** (Re)charge en appliquant query/range, puis **tri**. */
    private fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val flow = when {
                query.isNotBlank() && range != null ->
                    repository.searchBetween("%$query%", range!!.first, range!!.second)
                query.isNotBlank() ->
                    repository.searchAll("%$query%")
                range != null ->
                    repository.getBetweenDates(range!!.first, range!!.second)
                else ->
                    repository.getAllFlow()
            }
            flow.collectLatest { list ->
                _tickets.value = list.applySort()
            }
        }
    }

    /** Applique le tri courant à une liste. */
    private fun List<Ticket>.applySort(): List<Ticket> = when (sortMode) {
        SortMode.DATE_DESC    -> this.sortedByDescending { it.dateMillis }
        SortMode.DATE_ASC     -> this.sortedBy { it.dateMillis }
        SortMode.AMOUNT_DESC  -> this.sortedByDescending { it.amount }
        SortMode.AMOUNT_ASC   -> this.sortedBy { it.amount }
    }

    /** Change le tri et réapplique sur la liste affichée. */
    fun setSort(mode: SortMode) {
        sortMode = mode
        _tickets.value = _tickets.value.applySort()
    }

    // ------- API publique identique (+ search) -------

    fun refresh() {
        range = null
        query = ""
        reload()
    }

    fun setDateRange(start: Long, end: Long) {
        range = start to end
        reload()
    }

    fun clearDateRange() {
        range = null
        reload()
    }

    fun setQuery(q: String) {
        query = q
        reload()
    }

    // CRUD
    fun insert(ticket: Ticket) = viewModelScope.launch { repository.insert(ticket) }
    fun delete(ticket: Ticket) = viewModelScope.launch { repository.delete(ticket) }
    fun update(ticket: Ticket) = viewModelScope.launch { repository.update(ticket) }
}