package com.etix.features.budget

import com.etix.features.ticket.TicketFormRules
import java.util.Locale

/** État d'une ligne budget — iOS CategoryRowView.BudgetStatus (seuils 80 % et 100 %). */
enum class BudgetStatus { OK, WARNING, EXCEEDED }

/** Budget d'une catégorie sur le mois courant. */
data class BudgetLine(val limit: Double, val spent: Double) {
    /** iOS : total / limite (limite > 0 garantie par le stockage). */
    val ratio: Double get() = spent / limit
    /** Montant restant ; négatif = dépassement (iOS l'expose dans BudgetSummaryEngine.remaining). */
    val remaining: Double get() = limit - spent
    val overrun: Double get() = (spent - limit).coerceAtLeast(0.0)
    val status: BudgetStatus get() = BudgetRules.status(ratio)
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

    /** iOS statusLabel : « Attention — 85% » / « Dépassé — 112% » ; aucun libellé si correct. */
    fun statusLabel(line: BudgetLine): String? = when (line.status) {
        BudgetStatus.OK -> null
        BudgetStatus.WARNING -> String.format(Locale.FRANCE, "Attention — %.0f%%", line.ratio * 100)
        BudgetStatus.EXCEEDED -> String.format(Locale.FRANCE, "Dépassé — %.0f%%", line.ratio * 100)
    }

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
    fun accessibilityText(line: BudgetLine): String = if (line.remaining >= 0)
        "Budget ${formatEuro(line.limit)}, reste ${formatEuro(line.remaining)}"
    else "Budget ${formatEuro(line.limit)}, dépassé de ${formatEuro(line.overrun)}"

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
}
