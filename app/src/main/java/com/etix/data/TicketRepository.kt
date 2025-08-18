package com.etix.data

import android.content.Context
import com.etix.model.CategoryTotal
import com.etix.model.Ticket
import kotlinx.coroutines.flow.Flow
import androidx.room.Room

class TicketRepository(context: Context) {

    private val ticketDao: TicketDao

    init {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "etix_db"
        )
            .fallbackToDestructiveMigration() // 🔧 Résout le problème "no such table"
            .build()

        ticketDao = db.ticketDao()
    }

    fun insert(ticket: Ticket) {
        ticketDao.insert(ticket)
    }

    fun getAllTickets(): Flow<List<Ticket>> {
        return ticketDao.getAllTickets()
    }

    fun getTicketsBetween(startDate: Long, endDate: Long): Flow<List<Ticket>> {
        return ticketDao.getTicketsBetween(startDate, endDate)
    }

    fun getCategoryTotals(): Flow<List<CategoryTotal>> {
        return ticketDao.getCategoryTotals()
    }
}
