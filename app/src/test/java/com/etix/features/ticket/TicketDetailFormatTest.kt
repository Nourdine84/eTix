package com.etix.features.ticket

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TicketDetailFormatTest {
    private val d = Calendar.getInstance().apply { clear(); set(2026, Calendar.SEPTEMBER, 8, 18, 42) }.timeInMillis

    @Test fun carte_date_comme_ios() {
        assertEquals("08", TicketDetailFormat.day(d))
        assertEquals("Septembre 2026", TicketDetailFormat.monthYear(d))
        assertEquals("Mardi", TicketDetailFormat.weekday(d))
    }

    @Test fun montant() {
        assertEquals("15,75 €", TicketDetailFormat.amount(15.75))
        assertEquals("1234,50 €", TicketDetailFormat.amount(1234.5))
    }
}
