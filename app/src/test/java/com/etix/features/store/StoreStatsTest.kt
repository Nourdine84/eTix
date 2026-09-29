package com.etix.features.store

import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class StoreStatsTest {

    /** 15 mars 2026, 12:00 locale — milieu de mois pour éviter les effets de bord. */
    private val now = cal(2026, Calendar.MARCH, 15, 12)

    private fun cal(y: Int, m: Int, d: Int, h: Int = 10): Long =
        Calendar.getInstance().apply { clear(); set(y, m, d, h, 0, 0) }.timeInMillis

    private var nextId = 1L
    private fun t(store: String, amount: Double, date: Long, category: String = "Courses") =
        Ticket(id = nextId++, store = store, amount = amount, category = category, dateMillis = date)

    @Test
    fun month_range_is_half_open_and_covers_whole_month() {
        val r = TimeRange.MONTH.currentRange(now)
        assertEquals(cal(2026, Calendar.MARCH, 1, 0), r.first)
        assertEquals(cal(2026, Calendar.APRIL, 1, 0) - 1, r.last)
        val p = TimeRange.MONTH.previousRange(now)
        assertEquals(cal(2026, Calendar.FEBRUARY, 1, 0), p.first)
        assertEquals(cal(2026, Calendar.MARCH, 1, 0) - 1, p.last)
    }

    @Test
    fun year_and_today_ranges() {
        assertEquals(cal(2026, Calendar.JANUARY, 1, 0), TimeRange.YEAR.currentRange(now).first)
        assertEquals(cal(2026, Calendar.MARCH, 15, 0), TimeRange.TODAY.currentRange(now).first)
        assertEquals(cal(2026, Calendar.MARCH, 16, 0) - 1, TimeRange.TODAY.currentRange(now).last)
    }

    @Test
    fun totals_group_case_and_whitespace_insensitive_sorted_by_total() {
        val tickets = listOf(
            t("Lidl", 10.0, cal(2026, Calendar.MARCH, 2)),
            t("LIDL ", 5.5, cal(2026, Calendar.MARCH, 10)),
            t("Carrefour", 30.0, cal(2026, Calendar.MARCH, 3)),
            t("Carrefour", 99.0, cal(2026, Calendar.FEBRUARY, 27)), // hors période
            t("   ", 12.0, cal(2026, Calendar.MARCH, 4)),           // magasin vide ignoré
        )
        val totals = StoreStats.storeTotals(tickets, TimeRange.MONTH.currentRange(now))

        assertEquals(listOf("Carrefour", "LIDL"), totals.map { it.storeName })
        assertEquals(30.0, totals[0].total, 0.001)
        assertEquals(15.5, totals[1].total, 0.001)
        assertEquals(2, totals[1].ticketCount)
        assertEquals(7.75, totals[1].averageBasket, 0.001)
        assertEquals(cal(2026, Calendar.MARCH, 10), totals[1].lastPurchaseMillis)
        assertEquals(34.07, StoreStats.sharePercent(totals[1], totals.sumOf { it.total }), 0.01)
    }

    @Test
    fun empty_period_gives_empty_list() {
        val tickets = listOf(t("Lidl", 10.0, cal(2025, Calendar.DECEMBER, 2)))
        assertEquals(emptyList<StoreTotal>(), StoreStats.storeTotals(tickets, TimeRange.MONTH.currentRange(now)))
    }

    @Test
    fun relative_last_visit_labels() {
        assertEquals("Dernier passage aujourd'hui", StoreStats.relativeLastVisit(cal(2026, Calendar.MARCH, 15, 8), now))
        assertEquals("Dernier passage hier", StoreStats.relativeLastVisit(cal(2026, Calendar.MARCH, 14, 23), now))
        assertEquals("Dernier passage il y a 14 jours", StoreStats.relativeLastVisit(cal(2026, Calendar.MARCH, 1), now))
        assertEquals("—", StoreStats.relativeLastVisit(0, now))
    }

    @Test
    fun detail_stats_match_ios_rules() {
        val tickets = listOf(
            t("Lidl", 20.0, cal(2026, Calendar.MARCH, 12), "Courses"),
            t("lidl", 10.0, cal(2026, Calendar.MARCH, 2), "Maison"),
            t("Lidl", 25.0, cal(2026, Calendar.FEBRUARY, 10), "Courses"),
            t("Lidl", 5.0, cal(2026, Calendar.JANUARY, 11), ""),
            t("Carrefour", 50.0, cal(2026, Calendar.MARCH, 5)),
        )
        val d = StoreStats.detail(tickets, StoreStats.keyOf("LIDL"), now)!!

        assertEquals("Lidl", d.storeName)
        assertEquals(60.0, d.total, 0.001)
        assertEquals(4, d.ticketCount)
        assertEquals(15.0, d.averageBasket, 0.001)
        assertEquals(cal(2026, Calendar.MARCH, 12), d.lastVisitMillis)
        // 11 janv → 12 mars = 60 jours, 3 intervalles → tous les 20 j
        assertEquals(20, d.avgDaysBetweenVisits)
        assertEquals(30.0, d.thisMonthTotal, 0.001)
        assertEquals(25.0, d.lastMonthTotal, 0.001)
        assertEquals(20.0, d.variationPercent, 0.001)
        assertEquals(listOf("Courses", "Maison", "Autre"), d.topCategories.map { it.name })
        assertEquals(75.0, d.topCategories[0].percent, 0.001)
        // tickets triés du plus récent au plus ancien
        assertEquals(cal(2026, Calendar.JANUARY, 11), d.tickets.last().dateMillis)
    }

    @Test
    fun detail_single_ticket_has_no_frequency_and_unknown_store_is_null() {
        val tickets = listOf(t("Esso", 40.0, cal(2026, Calendar.MARCH, 1)))
        val d = StoreStats.detail(tickets, "esso", now)!!
        assertNull(d.avgDaysBetweenVisits)
        assertEquals(0.0, d.variationPercent, 0.001)
        assertNull(StoreStats.detail(tickets, "inconnu", now))
    }
}
