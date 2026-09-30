package com.etix.features.home

import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import java.util.Calendar

/**
 * Portage des moteurs iOS de l'Accueil (feature/home-hero-v2) :
 * HomeSnapshot (sous-ensemble utile), FinancialStateEngine, HomeFinancialCopy, TrendEngine.
 * Fonctions pures, sans Android, testées en JVM.
 *
 * BudgetSummaryEngine : features/budget (lot 8). Non porté : HomeInsightEngine, StoreIntelligenceEngine (cartes contextuelles)
 * — voir docs/SUIVI_ANDROID.md.
 */
data class HomeSnapshot(
    val periodTotal: Double,
    val periodTicketCount: Int,
    val previousPeriodTotal: Double,
    val allTimeTicketCount: Int
) {
    /** iOS : comparaison exploitable = période précédente > 0, ticket courant, ≥ 3 tickets au total. */
    val hasComparison: Boolean
        get() = previousPeriodTotal > 0 && periodTicketCount > 0 && allTimeTicketCount >= 3

    /** Variation en %, null si pas de comparaison exploitable (jamais de faux chiffre). */
    val deltaPercent: Double?
        get() = if (hasComparison) (periodTotal - previousPeriodTotal) / previousPeriodTotal * 100 else null

    val averageBasket: Double
        get() = if (periodTicketCount > 0) periodTotal / periodTicketCount else 0.0

    companion object {
        fun of(tickets: List<Ticket>, range: TimeRange, now: Long = System.currentTimeMillis()): HomeSnapshot {
            val cur = range.currentRange(now)
            val prev = range.previousRange(now)
            var total = 0.0
            var count = 0
            var prevTotal = 0.0
            for (t in tickets) {
                when (t.dateMillis) {
                    in cur -> { total += t.amount; count++ }
                    in prev -> prevTotal += t.amount
                }
            }
            return HomeSnapshot(total, count, prevTotal, tickets.size)
        }
    }
}

enum class FinancialStateKind { WELCOME, BUILDING, SAVING, UNDER_CONTROL, STEADY, SLIGHT_RISE, HIGH_SPENDING }
enum class FinancialTone { POSITIVE, NEUTRAL, ATTENTION }
data class FinancialState(val kind: FinancialStateKind, val tone: FinancialTone)

/** iOS FinancialStateEngine — mêmes seuils (+30 %, −15 %, +10 %). */
object FinancialStateEngine {

    fun evaluate(snap: HomeSnapshot, budgetTense: Boolean = false): FinancialState {
        if (snap.allTimeTicketCount == 0) return FinancialState(FinancialStateKind.WELCOME, FinancialTone.NEUTRAL)
        if (!snap.hasComparison) return FinancialState(FinancialStateKind.BUILDING, FinancialTone.NEUTRAL)

        val delta = (snap.periodTotal - snap.previousPeriodTotal) / snap.previousPeriodTotal
        return when {
            delta >= 0.30 || budgetTense -> FinancialState(FinancialStateKind.HIGH_SPENDING, FinancialTone.ATTENTION)
            delta <= -0.15 -> FinancialState(FinancialStateKind.SAVING, FinancialTone.POSITIVE)
            delta >= 0.10 -> FinancialState(FinancialStateKind.SLIGHT_RISE, FinancialTone.NEUTRAL)
            delta < 0 -> FinancialState(FinancialStateKind.UNDER_CONTROL, FinancialTone.POSITIVE)
            else -> FinancialState(FinancialStateKind.STEADY, FinancialTone.NEUTRAL)
        }
    }
}

/** iOS HomeFinancialCopy + libellés dérivés de HomeView. */
object HomeCopy {

    fun narration(state: FinancialState, range: TimeRange): String = when (state.kind) {
        FinancialStateKind.WELCOME -> "Ajoute ton premier ticket"
        FinancialStateKind.BUILDING -> "Ton suivi prend forme"
        FinancialStateKind.SAVING -> "Tu dépenses moins que d'habitude"
        FinancialStateKind.UNDER_CONTROL -> "Belle maîtrise ${period(range)}"
        FinancialStateKind.STEADY -> "Tes dépenses sont stables"
        FinancialStateKind.SLIGHT_RISE -> "Légère hausse ${period(range)}"
        FinancialStateKind.HIGH_SPENDING -> "Ton rythme de dépenses augmente"
    }

    private fun period(range: TimeRange) = when (range) {
        TimeRange.TODAY -> "aujourd'hui"
        TimeRange.MONTH -> "ce mois-ci"
        TimeRange.YEAR -> "cette année"
    }

    fun greeting(hour: Int) = if (hour in 5 until 18) "Bonjour" else "Bonsoir"
    fun wish(hour: Int) = if (hour in 5 until 18) "Bonne journée" else "Bonne soirée"

    fun countLabel(count: Int): String = when {
        count == 0 -> "Aucun ticket enregistré"
        count == 1 -> "1 ticket enregistré"
        else -> "$count tickets enregistrés"
    }

    fun heroLabel(range: TimeRange) = when (range) {
        TimeRange.TODAY -> "Dépenses du jour"
        TimeRange.MONTH -> "Dépenses du mois"
        TimeRange.YEAR -> "Dépenses de l'année"
    }

    fun deltaLabel(range: TimeRange) = when (range) {
        TimeRange.TODAY -> "vs hier"
        TimeRange.MONTH -> "vs mois dernier"
        TimeRange.YEAR -> "vs an dernier"
    }
}

data class MonthlyTrendPoint(val month: String, val total: Double)

/** iOS TrendEngine : 6 derniers mois calendaires (mois courant inclus), mois vides à 0. */
object TrendEngine {

    /** Libellés iOS fr_FR shortStandaloneMonthSymbols, sans point, en majuscules. Figés pour ne pas
     *  dépendre des données de locale de l'appareil. */
    private val MONTHS = listOf("JANV", "FÉVR", "MARS", "AVR", "MAI", "JUIN", "JUIL", "AOÛT", "SEPT", "OCT", "NOV", "DÉC")

    fun monthlyTrend(tickets: List<Ticket>, monthsBack: Int = 6, now: Long = System.currentTimeMillis()): List<MonthlyTrendPoint> {
        if (monthsBack <= 0) return emptyList()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -(monthsBack - 1))
        }
        val starts = LongArray(monthsBack + 1)
        val labels = ArrayList<String>(monthsBack)
        for (i in 0..monthsBack) {
            starts[i] = cal.timeInMillis
            if (i < monthsBack) labels += MONTHS[cal.get(Calendar.MONTH)]
            cal.add(Calendar.MONTH, 1)
        }
        val totals = DoubleArray(monthsBack)
        for (t in tickets) {
            val ms = t.dateMillis
            if (ms < starts[0] || ms >= starts[monthsBack]) continue
            for (i in 0 until monthsBack) if (ms < starts[i + 1]) { totals[i] += t.amount; break }
        }
        return labels.mapIndexed { i, l -> MonthlyTrendPoint(l, totals[i]) }
    }
}
