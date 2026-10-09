package com.etix.features.budget

import com.etix.features.ticket.TicketFormRules
import java.util.Locale

/** État d'une ligne budget — iOS CategoryRowView.BudgetStatus (seuils 80 % et 100 %). */
enum class BudgetStatus { OK, WARNING, EXCEEDED }

/**
 * Budget d'une ligne de l'écran Catégories. Si plusieurs catégories ne diffèrent que par la casse (« Courses »,
 * « courses »), elles partagent la même clé donc le même budget : [spent] est alors la consommation CUMULÉE de toutes
 * ces catégories et [sharedWith] liste les autres noms (catégories et tickets inchangés).
 */
data class RowBudget(val line: BudgetLine, val sharedWith: List<String>) {
    val isShared: Boolean get() = sharedWith.isNotEmpty()
}

/** Agrégat de budgets : chaque budget (clé) compté UNE seule fois. */
data class BudgetTotals(val totalBudget: Double, val totalSpent: Double, val budgetCount: Int) {
    val remaining: Double get() = totalBudget - totalSpent
}

/** Budget d'une catégorie sur le mois courant. */
data class BudgetLine(val limit: Double, val spent: Double) {
    /** iOS : total / limite (limite > 0 garantie par le stockage). */
    val ratio: Double get() = spent / limit
    /** Montant restant ; négatif = dépassement (iOS l'expose dans BudgetSummaryEngine.remaining). */
    val remaining: Double get() = limit - spent
    val overrun: Double get() = (spent - limit).coerceAtLeast(0.0)
    /**
     * Dépenses moins budget, au centime (montants exacts, pas le pourcentage arrondi) : évite les restes d'arrondi
     * des sommes (0,1 + 0,2 = 0,3 pile) et distingue 59,99 € / 60 € (sous le budget, « 100% » une fois arrondi).
     */
    val overCents: Long get() = Math.round(spent * 100) - Math.round(limit * 100)
    /** Dépenses égales au budget au centime près. */
    val isReached: Boolean get() = overCents == 0L
    val status: BudgetStatus get() = BudgetRules.status(this)
}

/**
 * Règles des budgets mensuels — portage iOS (BudgetStore, BudgetSettingsView, CategoryRowView, CategoryView).
 * Écarts et ambiguïtés : docs/BUDGETS.md.
 */
object BudgetRules {

    /** iOS BudgetStore : clé = nom de catégorie en minuscules (aucun autre traitement). */
    fun key(category: String): String = category.lowercase(Locale.ROOT)

    /**
     * Saisie du montant : règle de saisie Android des tickets (iOS AmountParser) — espaces retirés, virgule ou point,
     * strictement positif. iOS BudgetEditSheet ne retire pas les espaces (« 1 200 » refusé sur iOS, accepté ici).
     * « ,20 » est accepté (0,20 €) comme pour les tickets : décision en attente (voir docs/BUDGETS.md).
     */
    fun parseLimit(raw: String): Double? = TicketFormRules.parseAmount(raw)

    /** iOS : ratio ≥ 1 → dépassé ; ≥ 0,8 → attention ; sinon correct. */
    fun status(ratio: Double): BudgetStatus = when {
        ratio >= 1.0 -> BudgetStatus.EXCEEDED
        ratio >= 0.8 -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }

    /**
     * État d'une ligne : 100 % et plus décidés au centime ([BudgetLine.overCents]), seuil d'alerte de 80 % inchangé.
     * Dépenses égales au budget : état EXCEEDED conservé (couleur rouge, comme sur iOS et sur la carte de l'Accueil),
     * seul le libellé change ([statusLabel]).
     */
    fun status(line: BudgetLine): BudgetStatus =
        if (line.overCents >= 0) BudgetStatus.EXCEEDED else status(line.ratio).let { if (it == BudgetStatus.EXCEEDED) BudgetStatus.WARNING else it }

    /**
     * iOS statusLabel : « Attention — 85% » / « Dépassé — 112% » ; aucun libellé si correct. Écart volontaire avec iOS
     * (décision du 09/10/2026, comme la carte de l'Accueil) : « Budget atteint » quand les dépenses égalent exactement le
     * budget ; « Dépassé » seulement s'il est réellement dépassé (≥ 1 centime). Le pourcentage reste arrondi (%.0f) :
     * 59,99 € / 60 € affiche « Attention — 100% », 60,01 € / 60 € « Dépassé — 100% ».
     */
    fun statusLabel(line: BudgetLine): String? = when (line.status) {
        BudgetStatus.OK -> null
        BudgetStatus.WARNING -> String.format(Locale.FRANCE, "Attention — %.0f%%", line.ratio * 100)
        BudgetStatus.EXCEEDED -> if (line.isReached) REACHED_LABEL
            else String.format(Locale.FRANCE, "Dépassé — %.0f%%", line.ratio * 100)
    }

    /** Libellé de l'Accueil et de Catégories quand les dépenses égalent exactement le budget. */
    const val REACHED_LABEL = "Budget atteint"

    /**
     * Montant affiché (liste, barre, champ prérempli). iOS arrondit à l'euro (« %.0f ») : un budget de 12,50 €
     * s'affiche « 13 € » et le champ est prérempli « 13 » (enregistré à 13 si l'on valide sans retoucher).
     * Android : décimales conservées quand elles existent (« 12,50 € »), entier sinon (« 300 € »).
     */
    fun formatEuro(v: Double): String = formatNumber(v) + " €"

    fun formatNumber(v: Double): String =
        if (v == Math.rint(v)) String.format(Locale.FRANCE, "%.0f", v) else String.format(Locale.FRANCE, "%.2f", v)

    /** iOS barre : « 85 € / 100 € ». */
    fun progressLabel(line: BudgetLine): String = "${formatEuro(line.spent)} / ${formatEuro(line.limit)}"

    /** Texte d'accessibilité : restant ou dépassement explicites (non affichés visuellement sur iOS). */
    fun accessibilityText(line: BudgetLine): String = if (line.overCents <= 0)
        "Budget ${formatEuro(line.limit)}, reste ${formatEuro(-line.overCents / 100.0)}"
    else "Budget ${formatEuro(line.limit)}, dépassé de ${formatEuro(line.overCents / 100.0)}"

    /** Barre : progression bornée à [0, 1] (iOS min(ratio, 1)). */
    fun progress(line: BudgetLine): Double = line.ratio.coerceIn(0.0, 1.0)

    /**
     * iOS BudgetSettingsView.loadCategories : catégories DISTINCTES présentes dans les tickets (nom exact),
     * non vides, triées. « Courses » et « courses » apparaissent toutes deux et partagent le même budget (clé minuscule).
     */
    fun settingsCategories(ticketCategories: Collection<String>): List<String> =
        ticketCategories.filter { it.isNotEmpty() }.distinct().sorted()

    /** Budget applicable à une ligne de l'écran Catégories : seulement sur « Ce mois » (iOS range == .month). */
    fun limitForRow(budgets: Map<String, Double>, category: String, isMonth: Boolean): Double? =
        if (isMonth) budgets[key(category)]?.takeIf { it > 0 } else null

    /**
     * Budgets des lignes de Catégories (« Ce mois » uniquement). Consommation = somme des totaux du mois de TOUTES les
     * catégories de même clé (budget partagé) ; chaque ligne garde son propre total affiché ailleurs.
     * Clé du résultat : nom exact de la catégorie de la ligne.
     */
    fun rowBudgets(rows: List<Pair<String, Double>>, budgets: Map<String, Double>, isMonth: Boolean): Map<String, RowBudget> {
        if (!isMonth) return emptyMap()
        val byKey = rows.groupBy { key(it.first) }
        return rows.mapNotNull { (name, _) ->
            val k = key(name)
            val limit = budgets[k]?.takeIf { it > 0 } ?: return@mapNotNull null
            val group = byKey.getValue(k)
            name to RowBudget(BudgetLine(limit, group.sumOf { it.second }),
                group.map { it.first }.filter { it != name })
        }.toMap()
    }

    /**
     * Agrégat de tous les budgets (futur « total des budgets », iOS BudgetSummaryEngine) : chaque clé de budget comptée
     * UNE fois ; dépenses = uniquement catégories budgétées, cumulées par clé (casse ignorée). Non affiché à ce jour.
     */
    fun totals(rows: List<Pair<String, Double>>, budgets: Map<String, Double>): BudgetTotals {
        val valid = budgets.filterValues { it > 0 }
        val spentByKey = rows.groupBy({ key(it.first) }, { it.second }).mapValues { it.value.sum() }
        return BudgetTotals(valid.values.sum(), valid.keys.sumOf { spentByKey[it] ?: 0.0 }, valid.size)
    }

    /** « Budget partagé avec « courses » » ; plusieurs noms séparés par des virgules. */
    fun sharedLabel(rb: RowBudget): String? =
        if (!rb.isShared) null else "Budget partagé avec " + rb.sharedWith.joinToString(", ") { "« $it »" }
}
