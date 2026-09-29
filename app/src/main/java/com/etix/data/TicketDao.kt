package com.etix.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.etix.model.CategoryTotal
import com.etix.model.Ticket
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    // --------------------
    // CRUD
    // --------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: Ticket)

    @Update
    suspend fun update(ticket: Ticket)

    @Delete
    suspend fun delete(ticket: Ticket)

    // --------------------
    // Flux de base
    // --------------------
    @Query("SELECT * FROM tickets ORDER BY dateMillis DESC")
    fun getAllFlow(): Flow<List<Ticket>>

    @Query("""
        SELECT * FROM tickets
        WHERE dateMillis BETWEEN :start AND :end
        ORDER BY dateMillis DESC
    """)
    fun getBetweenDates(start: Long, end: Long): Flow<List<Ticket>>

    // --------------------
    // Recherche texte
    // --------------------
    @Query("""
        SELECT * FROM tickets
        WHERE (store LIKE '%' || :q || '%'
           OR  category LIKE '%' || :q || '%'
           OR  description LIKE '%' || :q || '%')
        ORDER BY dateMillis DESC
    """)
    fun searchAll(q: String): Flow<List<Ticket>>

    @Query("""
        SELECT * FROM tickets
        WHERE dateMillis BETWEEN :start AND :end
          AND (store LIKE '%' || :q || '%'
           OR  category LIKE '%' || :q || '%'
           OR  description LIKE '%' || :q || '%')
        ORDER BY dateMillis DESC
    """)
    fun searchBetween(q: String, start: Long, end: Long): Flow<List<Ticket>>

    // --------------------
    // Catégories
    // --------------------
    @Query("""
        SELECT category AS name, SUM(amount) AS total
        FROM tickets
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getTotalsByCategory(): LiveData<List<CategoryTotal>>

    @Query("""
        SELECT category AS name, SUM(amount) AS total
        FROM tickets
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getCategoryTotals(): Flow<List<CategoryTotal>>

    // --------------------
    // Lecture unitaire
    // --------------------
    /** Lot 4 : catégories déjà utilisées (sélecteur, parité iOS). Lecture seule, sans changement de schéma. */
    @Query("SELECT DISTINCT category FROM tickets")
    fun distinctCategoriesFlow(): Flow<List<String>>

    @Query("SELECT * FROM tickets WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<Ticket?>

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Ticket?

    // --------------------
    // KPI
    // --------------------
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM tickets
        WHERE dateMillis BETWEEN :start AND :end
    """)
    suspend fun sumBetweenDates(start: Long, end: Long): Double

    @Query("""
        SELECT COUNT(*)
        FROM tickets
        WHERE dateMillis BETWEEN :start AND :end
    """)
    suspend fun countBetweenDates(start: Long, end: Long): Int

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM tickets
        WHERE dateMillis BETWEEN :start AND :end
    """)
    fun sumBetweenDatesFlow(start: Long, end: Long): Flow<Double>

    @Query("""
        SELECT COUNT(*)
        FROM tickets
        WHERE dateMillis BETWEEN :start AND :end
    """)
    fun countBetweenDatesFlow(start: Long, end: Long): Flow<Int>

    // --------------------
    // Maintenance
    // --------------------
    @Query("DELETE FROM tickets")
    suspend fun deleteAll()
}
