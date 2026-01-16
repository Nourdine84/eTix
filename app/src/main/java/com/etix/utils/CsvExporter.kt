package com.etix.utils

import android.content.Context
import com.etix.model.Ticket
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun export(context: Context, tickets: List<Ticket>): File {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fileName = "etix_export_${System.currentTimeMillis()}.csv"

        val file = File(context.cacheDir, fileName)
        val writer = FileWriter(file)

        // Header
        writer.append("Date,Magasin,Montant,Catégorie,Description\n")

        tickets.forEach { ticket ->
            val date = sdf.format(Date(ticket.dateMillis))
            writer.append(
                "$date," +
                        "\"${ticket.store}\"," +
                        "${ticket.amount}," +
                        "\"${ticket.category}\"," +
                        "\"${ticket.description ?: ""}\"" +
                        "\n"
            )
        }

        writer.flush()
        writer.close()

        return file
    }
}
