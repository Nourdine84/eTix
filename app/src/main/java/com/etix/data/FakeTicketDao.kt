package com.etix.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.etix.model.CategoryTotal
import com.etix.model.Ticket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeTicketDao : TicketDao {

    private var autoId = 1L
    private val items = mutableListOf<Ticket>()
    private val flow = MutableStateFlow<List<Ticket>>(emptyList())
    private val totals = MutableLiveData<List<CategoryTotal>>(emptyList())

    private fun publish() {
        flow.value = items.sortedByDescending { it.dateMillis }

        totals.postValue(
            items.groupBy { it.category }
                .map { (cat, tickets) ->
                    CategoryTotal(cat, tickets.sumOf { it.amount })
                }
                .sortedByDescending { it.total }
        )
    }

    // ----------------------
    // CRUD
    // ----------------------

    override suspend fun insert(ticket: Ticket) {
        val id = if (ticket.id == 0L) autoId++ else ticket.id
        items += ticket.copy(id = id)
        publish()
    }

    override suspend fun update(ticket: Ticket) {
        val index = items.indexOfFirst { it.id == ticket.id }
        if (index >= 0) {
            items[index] = ticket
            publish()
        }
    }

    override suspend fun delete(ticket: Ticket) {
        items.removeAll { it.id == ticket.id }
        publish()
    }

    override suspend fun deleteAll() {
        items.clear()
        publish()
    }

    // ----------------------
    // LECTURE
    // ----------------------

    override suspend fun getById(id: Long): Ticket? =
        items.find { it.id == id }

    override fun getByIdFlow(id: Long): Flow<Ticket?> =
        flow.map { list -> list.find { it.id == id } }

    override fun getAllFlow(): Flow<List<Ticket>> = flow

    override fun distinctCategoriesFlow(): Flow<List<String>> =
        flow.map { list -> list.map { it.category }.distinct() }

    // ----------------------
    // DATES
    // ----------------------

    override fun getBetweenDates(start: Long, end: Long): Flow<List<Ticket>> =
        flow.map { list -> list.filter { it.dateMillis in start..end } }

    override suspend fun sumBetweenDates(start: Long, end: Long): Double =
        items.filter { it.dateMillis in start..end }.sumOf { it.amount }

    override suspend fun countBetweenDates(start: Long, end: Long): Int =
        items.count { it.dateMillis in start..end }

    // 🆕 FLOWS KPI (OBLIGATOIRES)
    override fun sumBetweenDatesFlow(start: Long, end: Long): Flow<Double> =
        flow.map { list ->
            list.filter { it.dateMillis in start..end }
                .sumOf { it.amount }
        }

    override fun countBetweenDatesFlow(start: Long, end: Long): Flow<Int> =
        flow.map { list ->
            list.count { it.dateMillis in start..end }
        }

    // ----------------------
    // CATÉGORIES
    // ----------------------

    override fun getTotalsByCategory(): LiveData<List<CategoryTotal>> = totals

    override fun getCategoryTotals(): Flow<List<CategoryTotal>> =
        flow.map { list ->
            list.groupBy { it.category }
                .map { (cat, tickets) ->
                    CategoryTotal(cat, tickets.sumOf { it.amount })
                }
                .sortedByDescending { it.total }
        }

    // ----------------------
    // RECHERCHE
    // ----------------------

    override fun searchAll(q: String): Flow<List<Ticket>> =
        flow.map { list ->
            list.filter {
                it.store.contains(q, ignoreCase = true) ||
                        (it.description?.contains(q, ignoreCase = true) == true)
            }
        }

    override fun searchBetween(q: String, start: Long, end: Long): Flow<List<Ticket>> =
        flow.map { list ->
            list.filter {
                it.dateMillis in start..end &&
                        (it.store.contains(q, ignoreCase = true) ||
                                (it.description?.contains(q, ignoreCase = true) == true))
            }
        }
}
