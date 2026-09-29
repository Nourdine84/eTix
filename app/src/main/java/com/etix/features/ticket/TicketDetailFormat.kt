package com.etix.features.ticket

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Textes de la carte date du détail (iOS TicketDetailView : « dd », « MMMM yyyy » et « EEEE » en fr_FR, capitalisés). */
object TicketDetailFormat {
    private val FR = Locale.FRANCE
    fun day(millis: Long): String = SimpleDateFormat("dd", FR).format(Date(millis))
    fun monthYear(millis: Long): String = cap(SimpleDateFormat("MMMM yyyy", FR).format(Date(millis)))
    fun weekday(millis: Long): String = cap(SimpleDateFormat("EEEE", FR).format(Date(millis)))
    fun amount(v: Double): String = String.format(FR, "%.2f €", v)
    private fun cap(s: String) = s.replaceFirstChar { it.titlecase(FR) }
}
