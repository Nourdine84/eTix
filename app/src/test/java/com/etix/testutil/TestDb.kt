package com.etix.testutil

import android.content.Context
import com.etix.data.AppDatabase
import com.etix.model.Ticket
import kotlinx.coroutines.runBlocking

/** Base Room de test : réinitialise le singleton (réutilisé entre tests Robolectric) et injecte des tickets. */
object TestDb {

    fun reset(context: Context) {
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (field.get(null) as AppDatabase?)?.close()
        field.set(null, null)
        context.deleteDatabase("etix.db")
    }

    fun seed(context: Context, tickets: List<Ticket>) = runBlocking {
        val dao = AppDatabase.getInstance(context).ticketDao()
        tickets.forEach { dao.insert(it) }
    }
}
