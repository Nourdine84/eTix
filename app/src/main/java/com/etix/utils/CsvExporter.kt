package com.etix.utils

import android.content.Context
import android.os.Environment
import com.etix.model.Ticket
import java.io.File
import java.io.FileWriter

object CsvExporter {

    fun exportTickets(context: Context, tickets: List<Ticket>): File {
        val csvFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "tickets.csv")
        val writer = FileWriter(csvFile)

        writer.append("Date,Magasin,Montant,Catégorie\n")
        tickets.forEach {
            writer.append("${it.dateMillis},${it.store},${it.amount},${it.category}\n")
        }

        writer.flush()
        writer.close()
        return csvFile
    }
}
