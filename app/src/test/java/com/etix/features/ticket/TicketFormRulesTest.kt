package com.etix.features.ticket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TicketFormRulesTest {

    @Test
    fun amount_parsing_matches_ios() {
        assertEquals(12.5, TicketFormRules.parseAmount("12,50")!!, 1e-9)
        assertEquals(12.5, TicketFormRules.parseAmount(" 12.50 ")!!, 1e-9)
        assertEquals(1234.5, TicketFormRules.parseAmount("1 234,5")!!, 1e-9)
        assertNull(TicketFormRules.parseAmount(""))
        assertNull(TicketFormRules.parseAmount("0"))
        assertNull(TicketFormRules.parseAmount("0,00"))
        assertNull(TicketFormRules.parseAmount("-3"))
        assertNull(TicketFormRules.parseAmount("12,50,1"))
        assertNull(TicketFormRules.parseAmount(","))
        assertEquals("12,50", TicketFormRules.formatAmountForInput(12.5))
    }

    @Test
    fun picker_lists_system_then_used_categories() {
        val list = TicketFormRules.pickerCategories(listOf("Courses", "Autre", "", "santé", "Santé", "animaux", "Courses "))
        assertEquals(TicketFormRules.SYSTEM_CATEGORIES, list.take(12))
        // « santé » (casse différente) reste une catégorie utilisée distincte, comme iOS (comparaison exacte)
        assertEquals(listOf("animaux", "Courses", "santé"), list.drop(12))
    }

    @Test
    fun picked_day_keeps_time_of_reference() {
        val ref = Calendar.getInstance().apply { clear(); set(2026, Calendar.SEPTEMBER, 29, 14, 35, 10) }.timeInMillis
        val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, Calendar.MARCH, 3) }.timeInMillis
        val out = Calendar.getInstance().apply { timeInMillis = TicketFormRules.combineDay(picked, ref) }
        assertEquals(2026, out.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, out.get(Calendar.MONTH))
        assertEquals(3, out.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, out.get(Calendar.HOUR_OF_DAY))
        assertEquals(35, out.get(Calendar.MINUTE))
        // aller-retour sélection initiale
        val sel = TicketFormRules.toPickerSelection(ref)
        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = sel }
        assertEquals(29, u.get(Calendar.DAY_OF_MONTH)); assertEquals(0, u.get(Calendar.HOUR_OF_DAY))
    }
}
