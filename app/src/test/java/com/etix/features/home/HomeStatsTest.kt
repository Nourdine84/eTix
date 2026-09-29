package com.etix.features.home

import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HomeStatsTest {

    private val now = cal(2026, Calendar.MARCH, 15, 12)
    private fun cal(y: Int, m: Int, d: Int, h: Int = 10): Long =
        Calendar.getInstance().apply { clear(); set(y, m, d, h, 0, 0) }.timeInMillis

    private var id = 1L
    private fun t(amount: Double, date: Long) =
        Ticket(id = id++, store = "S", amount = amount, category = "C", dateMillis = date)

    private fun snap(vararg t: Ticket) = HomeSnapshot.of(t.toList(), TimeRange.MONTH, now)
    private fun state(vararg t: Ticket) = FinancialStateEngine.evaluate(snap(*t))

    private val feb = cal(2026, Calendar.FEBRUARY, 10)
    private val mar = cal(2026, Calendar.MARCH, 5)

    @Test
    fun snapshot_splits_current_and_previous_period() {
        val s = snap(t(10.0, mar), t(5.0, mar), t(40.0, feb), t(99.0, cal(2025, Calendar.DECEMBER, 1)))
        assertEquals(15.0, s.periodTotal, 0.001)
        assertEquals(2, s.periodTicketCount)
        assertEquals(40.0, s.previousPeriodTotal, 0.001)
        assertEquals(4, s.allTimeTicketCount)
        assertEquals(7.5, s.averageBasket, 0.001)
        assertEquals(-62.5, s.deltaPercent!!, 0.001)
    }

    @Test
    fun welcome_and_building_before_any_financial_rule() {
        assertEquals(FinancialStateKind.WELCOME, state().kind)
        // < 3 tickets au total → building même avec une période précédente
        assertEquals(FinancialStateKind.BUILDING, state(t(10.0, mar), t(50.0, feb)).kind)
        // aucun ticket courant : 0 € n'est PAS une économie
        assertEquals(FinancialStateKind.BUILDING, state(t(10.0, feb), t(10.0, feb), t(10.0, feb)).kind)
        // pas de période précédente
        assertEquals(FinancialStateKind.BUILDING, state(t(1.0, mar), t(1.0, mar), t(1.0, mar)).kind)
        assertNull(snap(t(1.0, mar), t(1.0, mar), t(1.0, mar)).deltaPercent)
        assertFalse(snap(t(1.0, mar)).hasComparison)
    }

    @Test
    fun thresholds_match_ios() {
        fun kind(current: Double, previous: Double) =
            state(t(current, mar), t(previous, feb), t(0.0, cal(2025, Calendar.JUNE, 1))).kind
        assertEquals(FinancialStateKind.HIGH_SPENDING, kind(130.0, 100.0))   // +30 %
        assertEquals(FinancialStateKind.SLIGHT_RISE, kind(129.0, 100.0))     // +29 %
        assertEquals(FinancialStateKind.SLIGHT_RISE, kind(110.0, 100.0))     // +10 %
        assertEquals(FinancialStateKind.STEADY, kind(105.0, 100.0))          // +5 %
        assertEquals(FinancialStateKind.STEADY, kind(100.0, 100.0))          // 0 %
        assertEquals(FinancialStateKind.UNDER_CONTROL, kind(90.0, 100.0))    // −10 %
        assertEquals(FinancialStateKind.SAVING, kind(85.0, 100.0))           // −15 %
        assertEquals(FinancialTone.ATTENTION, FinancialStateEngine.evaluate(
            snap(t(100.0, mar), t(100.0, feb), t(1.0, cal(2025, Calendar.JUNE, 1))), budgetTense = true).tone)
    }

    @Test
    fun copy_matches_ios_strings() {
        val s = FinancialState(FinancialStateKind.UNDER_CONTROL, FinancialTone.POSITIVE)
        assertEquals("Belle maîtrise ce mois-ci", HomeCopy.narration(s, TimeRange.MONTH))
        assertEquals("Belle maîtrise aujourd'hui", HomeCopy.narration(s, TimeRange.TODAY))
        assertEquals("Ajoute ton premier ticket",
            HomeCopy.narration(FinancialState(FinancialStateKind.WELCOME, FinancialTone.NEUTRAL), TimeRange.YEAR))
        assertEquals("Bonjour", HomeCopy.greeting(5))
        assertEquals("Bonsoir", HomeCopy.greeting(18))
        assertEquals("Bonsoir", HomeCopy.greeting(4))
        assertEquals("Bonne soirée", HomeCopy.wish(23))
        assertEquals("Aucun ticket enregistré", HomeCopy.countLabel(0))
        assertEquals("1 ticket enregistré", HomeCopy.countLabel(1))
        assertEquals("12 tickets enregistrés", HomeCopy.countLabel(12))
        assertEquals("Dépenses de l'année", HomeCopy.heroLabel(TimeRange.YEAR))
        assertEquals("vs hier", HomeCopy.deltaLabel(TimeRange.TODAY))
    }

    @Test
    fun trend_covers_six_calendar_months_including_current_with_zero_months() {
        val tickets = listOf(
            t(10.0, cal(2026, Calendar.MARCH, 1)),
            t(5.0, cal(2026, Calendar.MARCH, 31, 23)),
            t(20.0, cal(2026, Calendar.JANUARY, 15)),
            t(7.0, cal(2025, Calendar.OCTOBER, 1, 0)),   // 1er mois inclus
            t(99.0, cal(2025, Calendar.SEPTEMBER, 30)),  // hors fenêtre
            t(99.0, cal(2026, Calendar.APRIL, 1, 0)),    // futur, hors fenêtre
        )
        val trend = TrendEngine.monthlyTrend(tickets, now = now)
        assertEquals(listOf("OCT", "NOV", "DÉC", "JANV", "FÉVR", "MARS"), trend.map { it.month })
        assertEquals(listOf(7.0, 0.0, 0.0, 20.0, 0.0, 15.0), trend.map { it.total })
        assertTrue(TrendEngine.monthlyTrend(emptyList(), now = now).all { it.total == 0.0 })
    }
}
