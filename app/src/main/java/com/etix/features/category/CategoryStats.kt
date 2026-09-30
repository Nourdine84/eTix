package com.etix.features.category

import com.etix.features.store.TimeRange
import com.etix.model.Ticket

/** Équivalent iOS `CategoryTotal` (Models/DTO/CategoryTotal.swift). */
data class CategoryTotal(
    /** Nom EXACT enregistré sur les tickets (aucune fusion, aucun renommage des catégories existantes). */
    val name: String,
    val total: Double,
    val previousTotal: Double,
    val ticketCount: Int
) {
    /** iOS CategoryRowView.deltaPercent : null si aucune dépense sur la période précédente. */
    val deltaPercent: Double?
        get() = if (previousTotal > 0) (total - previousTotal) / previousTotal * 100 else null
}

/** Répartition d'une période — iOS CategoryViewModel (categories + grandTotal). */
data class CategoryBreakdown(val categories: List<CategoryTotal>, val grandTotal: Double) {
    fun percent(c: CategoryTotal): Double = if (grandTotal > 0) c.total / grandTotal * 100 else 0.0
    val isEmpty: Boolean get() = categories.isEmpty()
}

/** Tickets d'une journée (iOS CategoryDetailView.groupedByDay). */
data class DaySection(val dayStart: Long, val tickets: List<Ticket>) {
    val total: Double get() = tickets.sumOf { it.amount }
}

/** Détail d'une catégorie sur une période — iOS CategoryDetailView. */
data class CategoryDetail(
    val name: String,
    /** Plus récent d'abord. */
    val tickets: List<Ticket>,
    val total: Double,
    /** Jours du plus récent au plus ancien, tickets du plus récent au plus ancien. */
    val days: List<DaySection>
) {
    val count: Int get() = tickets.size
    /** Graphique « Évolution journalière » : jours du plus ancien au plus récent. */
    val chart: List<Pair<Long, Double>> get() = days.reversed().map { it.dayStart to it.total }
}

object CategoryStats {

    /** Libellé affiché pour un ticket sans catégorie (ancien enregistrement). Le ticket n'est pas modifié. */
    const val UNCATEGORIZED_LABEL = "Sans catégorie"

    fun displayName(name: String): String = name.ifBlank { UNCATEGORIZED_LABEL }

    /**
     * iOS `CategoryViewModel.load` : période courante [début, fin[, regroupement par catégorie EXACTE,
     * total de la période précédente de même durée, tri par total décroissant (nom en cas d'égalité, pour un
     * ordre stable — iOS ne précise pas).
     */
    fun breakdown(tickets: List<Ticket>, range: TimeRange, now: Long = System.currentTimeMillis()): CategoryBreakdown {
        val current = range.currentRange(now)
        val previous = range.previousRange(now)
        val prevTotals = tickets.filter { it.dateMillis in previous }
            .groupBy { it.category }
            .mapValues { (_, l) -> l.sumOf { it.amount } }
        val list = tickets.filter { it.dateMillis in current }
            .groupBy { it.category }
            .map { (name, l) -> CategoryTotal(name, l.sumOf { it.amount }, prevTotals[name] ?: 0.0, l.size) }
            .sortedWith(compareByDescending<CategoryTotal> { it.total }.thenBy { it.name })
        return CategoryBreakdown(list, list.sumOf { it.total })
    }

    /** iOS : prédicat `category == nom` (exact), période [début, fin[, tri date décroissante, regroupement par jour. */
    fun detail(tickets: List<Ticket>, name: String, range: TimeRange, now: Long = System.currentTimeMillis()): CategoryDetail {
        val r = range.currentRange(now)
        val list = tickets.filter { it.category == name && it.dateMillis in r }.sortedByDescending { it.dateMillis }
        val days = list.groupBy { startOfDay(it.dateMillis) }
            .map { (d, l) -> DaySection(d, l) }
            .sortedByDescending { it.dayStart }
        return CategoryDetail(name, list, list.sumOf { it.amount }, days)
    }

    /** Titre de section : « Aujourd'hui », « Hier », sinon date moyenne de la langue du téléphone (comme iOS). */
    fun sectionTitle(dayStart: Long, now: Long = System.currentTimeMillis(), locale: java.util.Locale = java.util.Locale.getDefault()): String {
        val today = startOfDay(now)
        val yesterday = java.util.Calendar.getInstance().apply { timeInMillis = today; add(java.util.Calendar.DAY_OF_MONTH, -1) }.timeInMillis
        return when (dayStart) {
            today -> "Aujourd’hui"
            yesterday -> "Hier"
            else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale).format(java.util.Date(dayStart))
        }
    }

    fun startOfDay(ms: Long): Long = java.util.Calendar.getInstance().apply {
        timeInMillis = ms
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}
