package com.etix.data

import com.etix.model.CategoryTotal
import com.etix.model.Ticket
import kotlinx.coroutines.flow.Flow

class TicketRepository(
    private val dao: TicketDao
) {

    // --------------------
    // Streams
    // --------------------
    fun getAllFlow(): Flow<List<Ticket>> =
        dao.getAllFlow()

    fun getBetweenDates(start: Long, end: Long): Flow<List<Ticket>> =
        dao.getBetweenDates(start, end)

    // --------------------
    // Search
    // --------------------
    fun searchAll(query: String): Flow<List<Ticket>> =
        dao.searchAll(query)

    fun searchBetween(query: String, start: Long, end: Long): Flow<List<Ticket>> =
        dao.searchBetween(query, start, end)

    // --------------------
    // Categories
    // --------------------
    fun getTotalsByCategory() =
        dao.getTotalsByCategory()

    fun getCategoryTotals(): Flow<List<CategoryTotal>> =
        dao.getCategoryTotals()

    // --------------------
    // Single ticket
    // --------------------
    fun getByIdFlow(id: Long): Flow<Ticket?> =
        dao.getByIdFlow(id)

    suspend fun getById(id: Long): Ticket? =
        dao.getById(id)

    // --------------------
    // CRUD
    // --------------------
    suspend fun insert(ticket: Ticket) =
        dao.insert(ticket)

    suspend fun update(ticket: Ticket) =
        dao.update(ticket)

    suspend fun delete(ticket: Ticket) =
        dao.delete(ticket)

    suspend fun deleteAll() =
        dao.deleteAll()

    // --------------------
    // KPI (suspend)
    // --------------------
    suspend fun sumBetweenDates(start: Long, end: Long): Double =
        dao.sumBetweenDates(start, end)

    suspend fun countBetweenDates(start: Long, end: Long): Int =
        dao.countBetweenDates(start, end)

    // --------------------
    // KPI (Flow)
    // --------------------
    fun sumBetweenDatesFlow(start: Long, end: Long): Flow<Double> =
        dao.sumBetweenDatesFlow(start, end)

    fun countBetweenDatesFlow(start: Long, end: Long): Flow<Int> =
        dao.countBetweenDatesFlow(start, end)
}
