package com.etix.features.store

import com.etix.model.Ticket
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Périodes de filtrage — alignées sur iOS `TimeRange` (today / month / year).
 * Bornes [start, end[ en millis, calendrier local.
 */
enum class TimeRange(val title: String) {
    TODAY("Aujourd’hui"),
    MONTH("Ce mois"),
    YEAR("Cette année");

    fun currentRange(now: Long = System.currentTimeMillis()): LongRange = bounds(now, 0)

    fun previousRange(now: Long = System.currentTimeMillis()): LongRange = bounds(now, -1)

    private fun bounds(now: Long, shift: Int): LongRange {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val field = when (this) {
            TODAY -> Calendar.DAY_OF_MONTH
            MONTH -> { cal.set(Calendar.DAY_OF_MONTH, 1); Calendar.MONTH }
            YEAR -> { cal.set(Calendar.DAY_OF_YEAR, 1); Calendar.YEAR }
        }
        cal.add(field, shift)
        val start = cal.timeInMillis
        cal.add(field, 1)
        return start until cal.timeInMillis
    }

    companion object {
        /** iOS : AppSettings.defaultRange = .month par défaut. */
        val DEFAULT = MONTH
    }
}

/** Équivalent iOS `StoreTotal`. */
data class StoreTotal(
    val key: String,
    val storeName: String,
    val total: Double,
    val ticketCount: Int,
    val lastPurchaseMillis: Long
) {
    val averageBasket: Double get() = if (ticketCount > 0) total / ticketCount else 0.0
}

data class CategoryShare(val name: String, val total: Double, val percent: Double)

/** Statistiques de la fiche magasin — équivalent des propriétés calculées de iOS `StoreDetailView`. */
data class StoreDetailStats(
    val storeName: String,
    val total: Double,
    val ticketCount: Int,
    val averageBasket: Double,
    val lastVisitMillis: Long?,
    val avgDaysBetweenVisits: Int?,
    val thisMonthTotal: Double,
    val lastMonthTotal: Double,
    val variationPercent: Double,
    val topCategories: List<CategoryShare>,
    /** Tickets du magasin, du plus récent au plus ancien. */
    val tickets: List<Ticket>
)

object StoreStats {

    /**
     * Clé de regroupement : nom sans espaces superflus, insensible à la casse.
     * Écart assumé vs iOS (regroupement exact) : la saisie manuelle Android produit
     * facilement "Lidl" / "LIDL " pour un même magasin.
     */
    fun keyOf(storeName: String): String =
        storeName.trim().replace(Regex("\\s+"), " ").lowercase(Locale.FRANCE)

    /** iOS `StoreListViewModel.load` : filtre période, regroupe par magasin, tri par total décroissant. */
    fun storeTotals(tickets: List<Ticket>, range: LongRange): List<StoreTotal> =
        tickets
            .filter { it.dateMillis in range && it.store.isNotBlank() }
            .groupBy { keyOf(it.store) }
            .map { (key, list) ->
                val newest = list.maxBy { it.dateMillis }
                StoreTotal(
                    key = key,
                    storeName = newest.store.trim(),
                    total = list.sumOf { it.amount },
                    ticketCount = list.size,
                    lastPurchaseMillis = newest.dateMillis
                )
            }
            .sortedWith(compareByDescending<StoreTotal> { it.total }.thenBy { it.storeName })

    fun sharePercent(store: StoreTotal, grandTotal: Double): Double =
        if (grandTotal > 0) store.total / grandTotal * 100 else 0.0

    /** iOS `relativeLastVisit`. */
    fun relativeLastVisit(lastMillis: Long, now: Long = System.currentTimeMillis()): String {
        if (lastMillis <= 0) return "—"
        val days = daysBetween(lastMillis, now)
        return when {
            days <= 0 -> "Dernier passage aujourd'hui"
            days == 1 -> "Dernier passage hier"
            else -> "Dernier passage il y a $days jours"
        }
    }

    /** Fiche magasin : tous les tickets du magasin (toutes périodes), comme iOS. */
    fun detail(allTickets: List<Ticket>, storeKey: String, now: Long = System.currentTimeMillis()): StoreDetailStats? {
        val tickets = allTickets
            .filter { keyOf(it.store) == storeKey }
            .sortedByDescending { it.dateMillis }
        if (tickets.isEmpty()) return null

        val total = tickets.sumOf { it.amount }
        val count = tickets.size

        val thisMonth = TimeRange.MONTH.currentRange(now)
        val lastMonth = TimeRange.MONTH.previousRange(now)
        val thisMonthTotal = tickets.filter { it.dateMillis in thisMonth }.sumOf { it.amount }
        val lastMonthTotal = tickets.filter { it.dateMillis in lastMonth }.sumOf { it.amount }

        val avgDays = if (count >= 2) {
            val spanDays = ((tickets.first().dateMillis - tickets.last().dateMillis).toDouble() /
                    TimeUnit.DAYS.toMillis(1)).roundToInt()
            max(1, spanDays / (count - 1))
        } else null

        val byCategory = tickets
            .groupBy { it.category.ifBlank { "Autre" } }
            .map { (name, list) -> name to list.sumOf { it.amount } }
        val catTotal = byCategory.sumOf { it.second }
        val top = byCategory
            .map { (name, t) -> CategoryShare(name, t, if (catTotal > 0) t / catTotal * 100 else 0.0) }
            .sortedByDescending { it.total }
            .take(3)

        return StoreDetailStats(
            storeName = tickets.first().store.trim(),
            total = total,
            ticketCount = count,
            averageBasket = total / count,
            lastVisitMillis = tickets.first().dateMillis,
            avgDaysBetweenVisits = avgDays,
            thisMonthTotal = thisMonthTotal,
            lastMonthTotal = lastMonthTotal,
            variationPercent = if (lastMonthTotal > 0) (thisMonthTotal - lastMonthTotal) / lastMonthTotal * 100 else 0.0,
            topCategories = top,
            tickets = tickets
        )
    }

    private fun daysBetween(from: Long, to: Long): Int {
        fun startOfDay(ms: Long) = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((startOfDay(to) - startOfDay(from)).toDouble() / TimeUnit.DAYS.toMillis(1)).roundToInt()
    }
}
