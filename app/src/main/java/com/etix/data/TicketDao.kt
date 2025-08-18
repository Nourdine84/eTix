package com.etix.data

import androidx.room.*
import com.etix.model.Ticket
import com.etix.model.CategoryTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: Ticket)

    @Query("SELECT * FROM ticket ORDER BY date DESC")
    fun getAllTickets(): Flow<List<Ticket>>

    @Query("SELECT * FROM ticket WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTicketsBetween(startDate: String, endDate: String): Flow<List<Ticket>>

    @Query("SELECT category, SUM(amount) as totalAmount FROM ticket GROUP BY category")
    fun getCategoryTotals(): Flow<List<CategoryTotal>>
}
