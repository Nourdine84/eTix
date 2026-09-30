package com.etix.features.budget

import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import java.util.Calendar
import java.util.Locale

/** iOS BudgetState (Accueil) : seuils 50 / 80 / 100 % — différents de ceux des lignes Catégories (80 / 100 %). */
enum class HomeBudgetState {
    COMFORTABLE, CAUTION, CRITICAL, EXCEEDED;

    companion object {
        fun from(ratio: Double): HomeBudgetState = when {
            ratio >= 1.0 -> EXCEEDED
            ratio >= 0.80 -> CRITICAL
            ratio >= 0.50 -> CAUTION
            else -> COMFORTABLE
        }
    }
}

/** iOS BudgetLine (Accueil) : un budget (clé), dépenses cumulées de toutes les catégories de même clé. */
data class HomeBudgetLine(val categoryName: String, val limit: Double, val spent: Double) {
    val ratio: Double get() = spent / limit
    val state: HomeBudgetState get() = HomeBudgetState.from(ratio)
}

/** iOS BudgetSummary. */
data class HomeBudgetSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val daysLeftInMonth: Int,
    /** Au plus 3, triées par ratio décroissant. */
    val lines: List<HomeBudgetLine>,
    val extraCount: Int
) {
    val remaining: Double get() = totalBudget - totalSpent
    val globalRatio: Double get() = totalSpent / totalBudget
    val state: HomeBudgetState get() = HomeBudgetState.from(globalRatio)
    /** iOS HomeView : budgetTense = état critique ou dépassé → phrase « Ton rythme de dépenses augmente ». */
    val isTense: Boolean get() = state == HomeBudgetState.CRITICAL || state == HomeBudgetState.EXCEEDED
}

/**
 * iOS BudgetSummaryEngine + agrégats HomeSnapshot, toujours sur le MOIS COURANT (budgets mensuels) ; l'Accueil
 * n'affiche la carte que sur « Ce mois ». Chaque budget (clé en minuscules) compté UNE fois ; dépenses =
 * uniquement catégories budgétées, cumulées par clé (catégories ne différant que par la casse additionnées).
 * Lecture seule : tickets, catégories et budgets ne sont jamais modifiés.
 */
object BudgetSummaryEngine {

    fun compute(tickets: List<Ticket>, budgets: Map<String, Double>, now: Long = System.currentTimeMillis()): HomeBudgetSummary? {
        val valid = budgets.filterValues { it > 0 }
        if (valid.isEmpty()) return null

        val month = TimeRange.MONTH.currentRange(now)
        val spentByKey = HashMap<String, Double>()
        val displayName = HashMap<String, String>()
        // iOS : parcours du plus récent au plus ancien ; nom affiché = casse du ticket le plus récent du mois
        for (t in tickets.filter { it.dateMillis in month }.sortedByDescending { it.dateMillis }) {
            val k = BudgetRules.key(t.category)
            if (k.isEmpty()) continue
            spentByKey[k] = (spentByKey[k] ?: 0.0) + t.amount
            displayName.putIfAbsent(k, t.category)
        }

        val totals = BudgetRules.totals(spentByKey.map { it.key to it.value }, valid)
        val lines = valid.map { (k, limit) ->
            HomeBudgetLine(displayName[k] ?: capitalizeWords(k), limit, spentByKey[k] ?: 0.0)
        }.sortedWith(compareByDescending<HomeBudgetLine> { it.ratio }.thenBy { it.categoryName })

        return HomeBudgetSummary(
            totalBudget = totals.totalBudget,
            totalSpent = totals.totalSpent,
            daysLeftInMonth = daysLeftInMonth(now),
            lines = lines.take(3),
            extraCount = (lines.size - 3).coerceAtLeast(0)
        )
    }

    /** iOS : jours entre le début d'aujourd'hui et le 1er du mois suivant (dernier jour du mois → 1). */
    fun daysLeftInMonth(now: Long): Int {
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = TimeRange.MONTH.currentRange(now).last + 1
        return Math.round((end - today.timeInMillis) / 86_400_000.0).toInt().coerceAtLeast(0)
    }

    /** iOS `String.capitalized` : chaque mot avec une majuscule initiale. */
    private fun capitalizeWords(s: String) = s.split(" ").joinToString(" ") { w ->
        w.replaceFirstChar { it.titlecase(Locale.FRANCE) }
    }

    // ----- Textes de la carte (iOS BudgetSummaryCardView) -----

    /** « Il te reste X » ou, en dépassement, « Budgets dépassés de X ». */
    fun headline(s: HomeBudgetSummary): String =
        if (s.state == HomeBudgetState.EXCEEDED) "Budgets dépassés de ${BudgetRules.formatEuro(s.totalSpent - s.totalBudget)}"
        else "Il te reste ${BudgetRules.formatEuro(s.remaining)}"

    fun caption(s: HomeBudgetSummary): String =
        "${BudgetRules.formatEuro(s.totalSpent)} dépensés sur ${BudgetRules.formatEuro(s.totalBudget)} prévus"

    /** iOS `Int(ratio * 100)` : troncature (99,9 % → « 99% »). */
    fun percent(ratio: Double): String = "${(ratio * 100).toInt()}%"

    fun daysLeft(n: Int): String = if (n > 1) "$n jours restants dans le mois" else "$n jour restant dans le mois"

    fun more(extra: Int): String = if (extra > 1) "et $extra autres →" else "et $extra autre →"
}
