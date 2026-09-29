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
}
