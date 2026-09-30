package com.etix.features.category

import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CategoryStatsTest {

    /** 15 mars 2026 12:00, heure locale. */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.MARCH, 15, 12, 0) }.timeInMillis
    private fun at(y: Int, m: Int, d: Int, h: Int = 10, min: Int = 0, s: Int = 0, ms: Int = 0) =
        Calendar.getInstance().apply { clear(); set(y, m, d, h, min, s); set(Calendar.MILLISECOND, ms) }.timeInMillis
    private var id = 0L
    private fun t(cat: String, amount: Double, date: Long) =
        Ticket(id = ++id, store = "S", amount = amount, category = cat, dateMillis = date)

    @Test fun regroupement_exact_tri_decroissant_et_pourcentages() {
        val b = CategoryStats.breakdown(listOf(
            t("Courses", 30.0, at(2026, 2, 2)),
            t("Courses", 20.0, at(2026, 2, 10)),
            t("Restaurant", 25.0, at(2026, 2, 3)),
            t("courses", 5.0, at(2026, 2, 4)),      // casse différente : catégorie distincte (comme iOS)
            t("Transport", 99.0, at(2026, 1, 20)),  // mois précédent : hors période
        ), TimeRange.MONTH, now)
        assertEquals(listOf("Courses", "Restaurant", "courses"), b.categories.map { it.name })
        assertEquals(80.0, b.grandTotal, 1e-9)
        assertEquals(62.5, b.percent(b.categories[0]), 1e-9)
        assertEquals(2, b.categories[0].ticketCount)
    }

    @Test fun variation_vs_periode_precedente() {
        val b = CategoryStats.breakdown(listOf(
            t("Courses", 120.0, at(2026, 2, 5)),
            t("Courses", 100.0, at(2026, 1, 5)),
            t("Loisirs", 30.0, at(2026, 2, 5)),
            t("Loisirs", 60.0, at(2026, 1, 28)),
            t("Santé", 10.0, at(2026, 2, 5)),
        ), TimeRange.MONTH, now)
        val by = b.categories.associateBy { it.name }
        assertEquals(20.0, by.getValue("Courses").deltaPercent!!, 1e-9)
        assertEquals(-50.0, by.getValue("Loisirs").deltaPercent!!, 1e-9)
        assertNull(by.getValue("Santé").deltaPercent) // rien le mois précédent → pas de variation (iOS)
    }

    @Test fun bornes_de_periode_debut_inclus_fin_exclue() {
        val b = CategoryStats.breakdown(listOf(
            t("A", 1.0, at(2026, 2, 1, 0, 0, 0, 0)),      // 1er mars 00:00:00.000 → inclus
            t("B", 2.0, at(2026, 1, 28, 23, 59, 59, 999)),// 28 fév. 23:59:59.999 → exclu (mois précédent)
            t("C", 4.0, at(2026, 2, 31, 23, 59, 59, 999)),// 31 mars 23:59:59.999 → inclus
            t("D", 8.0, at(2026, 3, 1, 0, 0, 0, 0)),      // 1er avril → exclu
        ), TimeRange.MONTH, now)
        assertEquals(setOf("A", "C"), b.categories.map { it.name }.toSet())
        assertEquals(2.0, CategoryStats.breakdown(listOf(t("B", 2.0, at(2026, 1, 28, 23, 59, 59, 999)),
            t("B", 3.0, at(2026, 2, 2))), TimeRange.MONTH, now).categories.single().previousTotal, 1e-9)
    }

    @Test fun aujourdhui_et_annee() {
        val tickets = listOf(t("Jour", 5.0, at(2026, 2, 15, 0)), t("Hier", 7.0, at(2026, 2, 14, 23, 59, 59, 999)),
            t("AnPasse", 9.0, at(2025, 11, 31, 23)))
        assertEquals(listOf("Jour"), CategoryStats.breakdown(tickets, TimeRange.TODAY, now).categories.map { it.name })
        assertEquals(setOf("Jour", "Hier"), CategoryStats.breakdown(tickets, TimeRange.YEAR, now).categories.map { it.name }.toSet())
    }

    @Test fun periode_vide_et_sans_categorie() {
        val empty = CategoryStats.breakdown(listOf(t("X", 3.0, at(2025, 0, 1))), TimeRange.MONTH, now)
        assertTrue(empty.isEmpty); assertEquals(0.0, empty.grandTotal, 0.0)
        val blank = CategoryStats.breakdown(listOf(t("", 3.0, at(2026, 2, 2))), TimeRange.MONTH, now)
        assertEquals("", blank.categories.single().name) // la donnée n'est pas modifiée
        assertEquals("Sans catégorie", CategoryStats.displayName(blank.categories.single().name))
    }

    @Test fun total_nul_pas_de_division_par_zero() {
        val b = CategoryStats.breakdown(listOf(t("Z", 0.0, at(2026, 2, 2))), TimeRange.MONTH, now)
        assertEquals(0.0, b.percent(b.categories.single()), 0.0)
    }

    // ---------- Détail d'une catégorie (lot 6, iOS CategoryDetailView) ----------

    @Test fun detail_categorie_exacte_periode_tri_et_jours() {
        val d = CategoryStats.detail(listOf(
            t("Courses", 10.0, at(2026, 2, 14, 9)),
            t("Courses", 5.0, at(2026, 2, 14, 18)),
            t("Courses", 7.0, at(2026, 2, 2, 12)),
            t("courses", 99.0, at(2026, 2, 14, 10)),   // casse différente : autre catégorie
            t("Courses", 50.0, at(2026, 1, 27)),        // mois précédent
            t("Restaurant", 20.0, at(2026, 2, 14)),
        ), "Courses", TimeRange.MONTH, now)
        assertEquals(3, d.count)
        assertEquals(22.0, d.total, 1e-9)
        assertEquals(listOf(5.0, 10.0, 7.0), d.tickets.map { it.amount })          // plus récent d'abord
        assertEquals(2, d.days.size)
        assertEquals(listOf(5.0, 10.0), d.days[0].tickets.map { it.amount })
        assertEquals(15.0, d.days[0].total, 1e-9)
        assertEquals(listOf(7.0, 15.0), d.chart.map { it.second })                // plus ancien d'abord
    }

    @Test fun detail_vide_et_sans_categorie() {
        val empty = CategoryStats.detail(listOf(t("Courses", 3.0, at(2025, 0, 1))), "Courses", TimeRange.MONTH, now)
        assertEquals(0, empty.count); assertTrue(empty.days.isEmpty()); assertTrue(empty.chart.isEmpty())
        val blank = CategoryStats.detail(listOf(t("", 3.0, at(2026, 2, 2)), t("Autre", 4.0, at(2026, 2, 2))), "", TimeRange.MONTH, now)
        assertEquals(listOf(3.0), blank.tickets.map { it.amount }) // « Sans catégorie » ≠ « Autre » : aucune fusion
    }

    @Test fun titres_de_section() {
        val fr = java.util.Locale.FRANCE
        assertEquals("Aujourd’hui", CategoryStats.sectionTitle(at(2026, 2, 15, 0), now, fr))
        assertEquals("Hier", CategoryStats.sectionTitle(at(2026, 2, 14, 0), now, fr))
        assertEquals(java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, fr).format(java.util.Date(at(2026, 2, 13, 0))),
            CategoryStats.sectionTitle(at(2026, 2, 13, 0), now, fr))
    }
}
