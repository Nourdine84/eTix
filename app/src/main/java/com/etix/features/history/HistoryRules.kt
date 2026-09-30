package com.etix.features.history

import com.etix.model.Ticket
import java.util.Calendar

/** Portage iOS TicketHistoryView : recherche, filtre de dates, regroupement, tri. */
object HistoryRules {

    data class Section(val label: String, val tickets: List<Ticket>)

    private fun startOfDay(ms: Long) = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * iOS : recherche insensible à la casse sur magasin OU catégorie ; début inclus (début de journée) ;
     * fin incluse (jusqu'au lendemain 0 h exclu). Tri : plus récent d'abord.
     */
    fun filter(tickets: List<Ticket>, query: String, startDay: Long?, endDay: Long?): List<Ticket> {
        val q = query.trim()
        val from = startDay?.let { startOfDay(it) }
        val to = endDay?.let {
            Calendar.getInstance().apply { timeInMillis = startOfDay(it); add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        }
        return tickets.filter { t ->
            (q.isEmpty() || t.store.contains(q, ignoreCase = true) || t.category.contains(q, ignoreCase = true)) &&
                (from == null || t.dateMillis >= from) &&
                (to == null || t.dateMillis < to)
        }.sortedWith(compareByDescending<Ticket> { it.dateMillis }.thenByDescending { it.id })
    }

    /** iOS : Aujourd'hui, Hier, Cette semaine, Ce mois, Plus ancien ; sections vides retirées. */
    fun group(sorted: List<Ticket>, now: Long = System.currentTimeMillis()): List<Section> {
        val today = startOfDay(now)
        val yesterday = Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_MONTH, -1) }.timeInMillis
        val week = Calendar.getInstance().apply {
            timeInMillis = today
            val diff = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
            add(Calendar.DAY_OF_MONTH, -diff)
        }.timeInMillis
        val month = Calendar.getInstance().apply { timeInMillis = today; set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

        val buckets = linkedMapOf<String, MutableList<Ticket>>(
            "Aujourd'hui" to mutableListOf(), "Hier" to mutableListOf(), "Cette semaine" to mutableListOf(),
            "Ce mois" to mutableListOf(), "Plus ancien" to mutableListOf()
        )
        for (t in sorted) {
            val day = startOfDay(t.dateMillis)
            val key = when {
                day >= today -> "Aujourd'hui"
                day >= yesterday -> "Hier"
                day >= week -> "Cette semaine"
                day >= month -> "Ce mois"
                else -> "Plus ancien"
            }
            buckets.getValue(key) += t
        }
        return buckets.filterValues { it.isNotEmpty() }.map { (k, v) -> Section(k, v) }
    }
}
