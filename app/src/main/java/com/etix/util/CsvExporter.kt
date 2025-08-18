package com.etix.util

import android.content.Context
import com.etix.model.Ticket
import java.io.File

object CsvExporter {

    /**
     * Exporte les tickets en CSV dans le répertoire privé de l'app (pas besoin de permission).
     * Retourne le File créé.
     */
    fun exportTickets(context: Context, tickets: List<Ticket>): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "etix_tickets_${System.currentTimeMillis()}.csv")

        file.bufferedWriter().use { out ->
            out.appendLine("store,date,amount,category,description")
            for (t in tickets) {
                // échappe les virgules / guillemets de base
                val store = t.store.replace("\"", "\"\"")
                val date = t.date
                val amount = t.amount.toString()
                val category = t.category.replace("\"", "\"\"")
                val desc = (t.description ?: "").replace("\"", "\"\"")
                out.appendLine("\"$store\",$date,$amount,\"$category\",\"$desc\"")
            }
        }
        return file
    }
}
