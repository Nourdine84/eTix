package com.etix.data

import androidx.lifecycle.asFlow
import com.etix.model.CategoryTotal
import com.etix.model.Ticket
import kotlinx.coroutines.flow.Flow

class TicketRepository(private val dao: TicketDao) {

    // --- Streams d'historique ---
    fun getAllFlow(): Flow<List<Ticket>> =
        dao.getAllFlow()

    fun getBetweenDates(start: Long, end: Long): Flow<List<Ticket>> =
        dao.getBetweenDates(start, end)

    // --- Recherche texte (store/description) ---
    fun searchAll(query: String): Flow<List<Ticket>> =
        dao.searchAll(query)

    fun searchBetween(query: String, start: Long, end: Long): Flow<List<Ticket>> =
        dao.searchBetween(query, start, end)

    // --- Totaux par catégorie ---
    // LiveData pour l’UI classique + Flow si tu préfères rester full-Flow
    fun getTotalsByCategory() = dao.getTotalsByCategory()                 // LiveData<List<CategoryTotal>>
    fun getCategoryTotals(): Flow<List<CategoryTotal>> =
        dao.getTotalsByCategory().asFlow()                                // Flow<List<CategoryTotal>>

    // --- Lecture unitaire ---
    fun getByIdFlow(id: Long): Flow<Ticket?> =
        dao.getByIdFlow(id)

    suspend fun getById(id: Long): Ticket? =
        dao.getById(id)

    // --- CRUD ---
    suspend fun insert(ticket: Ticket) = dao.insert(ticket)
    suspend fun update(ticket: Ticket) = dao.update(ticket)
    suspend fun delete(ticket: Ticket) = dao.delete(ticket)
    suspend fun deleteAll() = dao.deleteAll()

    // --- KPI ---
    suspend fun sumBetweenDates(start: Long, end: Long): Double =
        dao.sumBetweenDates(start, end)

    suspend fun countBetweenDates(start: Long, end: Long): Int =
        dao.countBetweenDates(start, end)
}