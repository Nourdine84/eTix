package com.etix.features.ocr.engine

import java.text.Normalizer
import java.util.Locale

/**
 * Choix du montant payé sur un texte de ticket (lot 6, correction du cas D1 iOS).
 *
 * Défaut corrigé : l'ancien moteur retenait la PREMIÈRE ligne contenant « TOTAL », donc « SOUS-TOTAL 42,80 »
 * au lieu de « TOTAL TTC 40,80 ». Règles, indépendantes des catégories :
 *  1. Lignes jamais retenues comme total : sous-total, total HT / HT, TVA, remise / réduction / bon / économie,
 *     rendu / à rendre, avoir.
 *  2. Lignes « total à payer », par priorité décroissante : NET À PAYER / TOTAL À PAYER / À PAYER,
 *     puis TOTAL TTC / MONTANT TTC, puis TOTAL seul. À priorité égale : la DERNIÈRE ligne du ticket.
 *     Montant pris en fin de ligne ; si la ligne n'a pas de montant et que la suivante n'est qu'un montant
 *     (colonnes séparées par l'OCR), on prend celui-ci.
 *  3. Sans ligne de total : montant payé (CB / carte / sans contact / chèque, dernière ligne) ;
 *     en espèces avec « rendu », espèces − rendu.
 *  4. Sinon : plus grand montant hors lignes exclues (comportement précédent, restreint).
 */
object OCRAmountExtractor {

    private val AMOUNT = Regex("""-?\d{1,3}(?:[  .]\d{3})*[.,]\d{2}(?!\d)|-?\d+[.,]\d{2}(?!\d)""")

    private val HARD_EXCLUDED = listOf(
        Regex("""\bSOUS[ -]?TOTAL\b"""), Regex("""\bS/TOTAL\b"""), Regex("""\bSUB[ -]?TOTAL\b"""),
        Regex("""\bREMISE\b"""), Regex("""\bREDUCTION\b"""), Regex("""\bECONOMIE"""), Regex("""\bBON\b"""),
        Regex("""\bRENDU\b"""), Regex("""\bA RENDRE\b"""), Regex("""\bAVOIR\b""")
    )
    private val SOFT_EXCLUDED = listOf(Regex("""\bH\.?T\.?(?=\s|$|\d)"""), Regex("""\bTVA\b"""))
    private val EXCLUDED = HARD_EXCLUDED + SOFT_EXCLUDED
    private val DUE_STRONG = Regex("""\b(NET|TOTAL|RESTE)? ?A PAYER\b""")
    private val DUE_TTC = Regex("""\b(TOTAL|MONTANT) ?T\.?T\.?C\b""")
    private val DUE_TOTAL = Regex("""\bTOTAL\b""")
    private val CARD = Regex("""\b(CB|CARTE|CARTE BANCAIRE|SANS CONTACT|VISA|MASTERCARD|CHEQUE|PAIEMENT|PAYE|REGLE)\b""")
    private val CASH = Regex("""\b(ESPECES|ESP|CASH)\b""")
    private val CHANGE = Regex("""\b(RENDU|A RENDRE|MONNAIE)\b""")

    /**
     * Montant retenu et sa provenance : [fromTotalLine] = ligne « total à payer » (règle 2), sinon repli.
     * [ambiguous] (lot 9) : plusieurs lignes de même priorité donnent des montants différents ; le montant retenu
     * est inchangé (dernière ligne), seule la confiance affichée baisse.
     */
    data class Hit(val value: Double, val fromTotalLine: Boolean, val ambiguous: Boolean = false)

    fun extract(rawLines: List<String>): Double? = extractDetailed(rawLines)?.value

    /** Mêmes règles que [extract] ; indique en plus si le montant vient d'une ligne de total (confiance iOS). */
    fun extractDetailed(rawLines: List<String>): Hit? {
        val lines = rawLines.map { it.trim() }.filter { it.isNotBlank() }
        val norm = lines.map(::normalize)

        fun isExcluded(i: Int) = EXCLUDED.any { it.containsMatchIn(norm[i]) }
        fun amountOf(i: Int): Double? = lastAmount(lines[i]) ?: lines.getOrNull(i + 1)
            ?.takeIf { isAmountOnly(it) }?.let(::lastAmount)

        // 2. Total à payer : premier montant APRÈS le mot-clé (« TOTAL TTC 40,80 DONT TVA 2,13 » → 40,80)
        for (rule in listOf(DUE_STRONG, DUE_TTC, DUE_TOTAL)) {
            val hits = norm.indices.mapNotNull { i ->
                val m = rule.find(norm[i]) ?: return@mapNotNull null
                if (isNotDue(norm[i])) return@mapNotNull null
                (firstAmountAfter(norm[i], m.range.last + 1) ?: lines.getOrNull(i + 1)
                    ?.takeIf { isAmountOnly(it) }?.let(::lastAmount))?.takeIf { it > 0 }
            }
            if (hits.isNotEmpty()) return Hit(hits.last(), true, ambiguous = hits.distinct().size > 1)
        }

        // 3. Montant payé
        norm.indices.filter { CARD.containsMatchIn(norm[it]) && !isExcluded(it) }
            .mapNotNull { amountOf(it)?.takeIf { v -> v > 0 } }.lastOrNull()?.let { return Hit(it, false) }
        val cash = norm.indices.filter { CASH.containsMatchIn(norm[it]) }.mapNotNull { amountOf(it) }.lastOrNull()
        if (cash != null && cash > 0) {
            val change = norm.indices.filter { CHANGE.containsMatchIn(norm[it]) }.mapNotNull { amountOf(it) }
                .lastOrNull() ?: 0.0
            return round2(cash - change).takeIf { it > 0 }?.let { Hit(it, false) }
        }

        // 4. Repli
        return norm.indices.filterNot(::isExcluded).mapNotNull { lastAmount(lines[it]) }
            .filter { it > 0.5 }.maxOrNull()?.let { Hit(it, false) }
    }

    /**
     * Ligne qui contient « TOTAL » sans être le total dû. Toujours : sous-total, remise, rendu, avoir.
     * HT / TVA : seulement si la ligne ne dit pas aussi TTC ou « à payer » (« TOTAL TTC … DONT TVA » reste valable).
     */
    private fun isNotDue(n: String): Boolean {
        if (HARD_EXCLUDED.any { it.containsMatchIn(n) }) return true
        val saysDue = DUE_STRONG.containsMatchIn(n) || DUE_TTC.containsMatchIn(n)
        return !saysDue && SOFT_EXCLUDED.any { it.containsMatchIn(n) }
    }

    /** Premier montant situé après la position [from] (ligne normalisée : chiffres inchangés). */
    private fun firstAmountAfter(line: String, from: Int): Double? =
        AMOUNT.findAll(line).firstOrNull { it.range.first >= from }?.value?.let(::parse)

    private fun isAmountOnly(line: String): Boolean =
        line.replace(AMOUNT, "").replace(Regex("""[€\sEUR]"""), "").isEmpty() && AMOUNT.containsMatchIn(line)

    private fun lastAmount(line: String): Double? =
        AMOUNT.findAll(line).lastOrNull()?.value?.let(::parse)

    internal fun parse(s: String): Double? {
        val t = s.replace(" ", "").replace(" ", "")
        val sep = maxOf(t.lastIndexOf(','), t.lastIndexOf('.'))
        if (sep < 0) return null
        val intPart = t.substring(0, sep).replace(".", "").replace(",", "")
        return "$intPart.${t.substring(sep + 1)}".toDoubleOrNull()?.let(::round2)
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").uppercase(Locale.ROOT)

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0
}
