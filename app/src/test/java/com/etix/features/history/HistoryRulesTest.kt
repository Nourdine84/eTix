package com.etix.features.history

import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class HistoryRulesTest {

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) =
        Calendar.getInstance().apply { clear(); set(y, m, d, h, 0) }.timeInMillis
    private var id = 1L
    private fun t(store: String, cat: String, date: Long) =
        Ticket(id = id++, store = store, amount = 1.0, category = cat, dateMillis = date)

    @Test
    fun search_matches_store_or_category_case_insensitive_sorted_desc() {
        val a = t("Lidl", "Alimentation", at(2026, 8, 1))
        val b = t("Esso", "Carburant", at(2026, 8, 3))
        val c = t("Pharmacie", "Santé", at(2026, 8, 2))
        val all = listOf(a, b, c)
        assertEquals(listOf(b, c, a), HistoryRules.filter(all, "", null, null))
        assertEquals(listOf(a), HistoryRules.filter(all, "lid", null, null))
        assertEquals(listOf(b), HistoryRules.filter(all, "CARBU", null, null))
        assertEquals(emptyList<Ticket>(), HistoryRules.filter(all, "zzz", null, null))
    }

    @Test
    fun date_filter_includes_start_and_end_days() {
        val d1 = t("A", "", at(2026, 8, 1, 0))
        val d2 = t("B", "", at(2026, 8, 2, 23))
        val d3 = t("C", "", at(2026, 8, 3, 0))
        val all = listOf(d1, d2, d3)
        assertEquals(listOf(d3, d2), HistoryRules.filter(all, "", at(2026, 8, 2, 18), null))
        assertEquals(listOf(d2, d1), HistoryRules.filter(all, "", null, at(2026, 8, 2, 8)))
        assertEquals(listOf(d2), HistoryRules.filter(all, "", at(2026, 8, 2), at(2026, 8, 2)))
    }

    @Test
    fun grouping_buckets_like_ios() {
        val saved = Locale.getDefault()
        Locale.setDefault(Locale.FRANCE) // semaine commençant le lundi
        try {
            val now = at(2026, Calendar.SEPTEMBER, 24, 15) // jeudi 24/09/2026
            val list = HistoryRules.filter(listOf(
                t("today", "", at(2026, 8, 24, 8)),
                t("yesterday", "", at(2026, 8, 23, 20)),
                t("monday", "", at(2026, 8, 21, 9)),
                t("sunday", "", at(2026, 8, 20, 9)),
                t("month", "", at(2026, 8, 1, 9)),
                t("old", "", at(2026, 7, 31, 9)),
            ), "", null, null)
            val groups = HistoryRules.group(list, now)
            assertEquals(listOf("Aujourd'hui", "Hier", "Cette semaine", "Ce mois", "Plus ancien"), groups.map { it.label })
            assertEquals(listOf("monday"), groups[2].tickets.map { it.store })
            assertEquals(listOf("sunday", "month"), groups[3].tickets.map { it.store })
            assertEquals(emptyList<HistoryRules.Section>(), HistoryRules.group(emptyList(), now))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
