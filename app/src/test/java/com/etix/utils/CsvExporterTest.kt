package com.etix.utils

import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Lot 10 — contenu du CSV exporté (format iOS SettingsViewModel.exportAllTickets) : en-tête, séparateur virgule,
 * montants à 2 décimales avec un point, dates jj/mm/aaaa dans le fuseau du téléphone, champs délicats protégés.
 */
class CsvExporterTest {

    private val paris = TimeZone.getTimeZone("Europe/Paris")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int, tz: TimeZone = paris): Long =
        Calendar.getInstance(tz).apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    @Test fun en_tete_seul_sans_ticket() {
        assertEquals("Date,Magasin,Montant (€),Catégorie,Description\n", CsvExporter.toCsv(emptyList(), paris))
    }

    @Test fun champs_delicats_proteges_et_ordre_du_plus_recent() {
        val tickets = listOf(
            Ticket(id = 1, store = "Café de l'Été", amount = 4.5, category = "Loisirs", description = null,
                dateMillis = at(2026, 1, 5, 9, 0)),
            Ticket(id = 2, store = "Durand, fils", amount = 1234.56, category = "Maison", description = "Vis \"inox\"",
                dateMillis = at(2026, 3, 31, 18, 0)),
            Ticket(id = 3, store = "Marché", amount = 0.05, category = "Courses", description = "ligne 1\nligne 2",
                dateMillis = at(2026, 2, 28, 12, 0)),
            Ticket(id = 4, store = "Boulangerie", amount = 12.0, category = "Alimentation", description = "a\r\nb",
                dateMillis = at(2026, 2, 28, 12, 0)),
        )
        val expected = "Date,Magasin,Montant (€),Catégorie,Description\n" +
            "31/03/2026,\"Durand, fils\",1234.56,Maison,\"Vis \"\"inox\"\"\"\n" +
            "28/02/2026,Marché,0.05,Courses,\"ligne 1\nligne 2\"\n" +
            "28/02/2026,Boulangerie,12.00,Alimentation,\"a\r\nb\"\n" +
            "05/01/2026,Café de l'Été,4.50,Loisirs,\n"
        assertEquals(expected, CsvExporter.toCsv(tickets, paris))
    }

    /** Date du jour LOCAL : 23 h 30 à Paris le 31/12 reste le 31/12 (et non le 01/01 de l'heure UTC +1). */
    @Test fun date_dans_le_fuseau_du_telephone() {
        val t = Ticket(id = 1, store = "Soir", amount = 1.0, category = "X", dateMillis = at(2025, 12, 31, 23, 30))
        assertEquals("31/12/2025", CsvExporter.toCsv(listOf(t), paris).lines()[1].substringBefore(','))
        assertEquals("31/12/2025", CsvExporter.toCsv(listOf(t), TimeZone.getTimeZone("UTC")).lines()[1].substringBefore(','))
        assertEquals("01/01/2026", CsvExporter.toCsv(listOf(t), TimeZone.getTimeZone("Asia/Tokyo")).lines()[1].substringBefore(','))
    }

    /** Montant toujours avec un point, quelle que soit la langue du téléphone (aucune virgule décimale ajoutée). */
    @Test fun montant_independant_de_la_langue() {
        val saved = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.FRANCE)
            val t = Ticket(id = 1, store = "A", amount = 3.4, category = "B", dateMillis = at(2026, 6, 1, 10, 0))
            assertEquals("01/06/2026,A,3.40,B,", CsvExporter.toCsv(listOf(t), paris).lines()[1])
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }

    @Test fun echappement() {
        assertEquals("simple", CsvExporter.escape("simple"))
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"\"\"\"", CsvExporter.escape("\""))
        assertEquals("\"x\ny\"", CsvExporter.escape("x\ny"))
        assertEquals("point-virgule;libre", CsvExporter.escape("point-virgule;libre"))
        assertEquals("", CsvExporter.escape(""))
    }
}
