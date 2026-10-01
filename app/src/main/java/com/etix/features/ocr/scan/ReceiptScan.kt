package com.etix.features.ocr.scan

import com.etix.features.ocr.engine.OCRAmountExtractor
import com.etix.features.ocr.engine.OCRCategoryGuesser
import com.etix.features.ocr.engine.OCRDateExtractor
import com.etix.features.ocr.engine.OCRProcessor
import com.etix.features.ticket.TicketFormRules
import com.etix.model.Ticket

/**
 * Niveau de confiance d'un champ lu sur un ticket — iOS `OCRConfidence` (paliers, jamais de pourcentage).
 * Affichage (iOS) : HIGH → « Vérifié », MEDIUM / LOW → « À vérifier », NONE → aucun badge.
 */
enum class ScanConfidence { HIGH, MEDIUM, LOW, NONE }

/** Champ lu + confiance. Invariant iOS : valeur absente ⇒ confiance NONE. */
data class ScanField<T>(val value: T?, val confidence: ScanConfidence) {
    companion object {
        fun <T> of(value: T?, confidence: ScanConfidence) =
            ScanField(value, if (value == null) ScanConfidence.NONE else confidence)
        fun <T> missing() = ScanField<T>(null, ScanConfidence.NONE)
    }
}

/** Résultat d'un scan — iOS `OCRExtractedData` + catégorie suggérée (dictionnaire iOS, lot 9). */
data class ReceiptScan(
    val store: ScanField<String>,
    val amount: ScanField<Double>,
    val date: ScanField<Long>,
    val category: OCRCategoryGuesser.Guess?
) {
    /** Aucune information exploitable — iOS : écran « Aucune information détectée ». */
    val isEmpty: Boolean get() = store.value == null && amount.value == null && date.value == null
}

/**
 * Texte reconnu → informations du ticket (iOS `ReceiptParser`), avec les règles OCR Android :
 * enseigne `OCRProcessor` (Q1), montant `OCRAmountExtractor` (lot 6), date `OCRDateExtractor` (Q3), catégorie
 * `OCRCategoryGuesser` (Q2). Confiances iOS : enseigne MEDIUM ; montant HIGH sur une ligne de total, sinon LOW ;
 * date HIGH.
 */
object ReceiptScanParser {
    fun parse(text: String): ReceiptScan {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val amount = OCRAmountExtractor.extractDetailed(lines)
        return ReceiptScan(
            store = ScanField.of(OCRProcessor.extractMerchant(lines), ScanConfidence.MEDIUM),
            amount = ScanField.of(amount?.value, if (amount?.fromTotalLine == true) ScanConfidence.HIGH else ScanConfidence.LOW),
            date = ScanField.of(OCRDateExtractor.extractDateMillis(lines), ScanConfidence.HIGH),
            category = OCRCategoryGuesser.guess(text)
        )
    }
}

/**
 * Catégorie proposée dans le formulaire après un scan — iOS `StoreCategoryMapper.suggest` :
 * 1. historique : catégorie la plus fréquente des tickets existants de ce magasin (casse ignorée), lecture seule ;
 *    ≥ 3 tickets → appliquée sans badge (iOS strongHistory), sinon badge « Suggéré par l'OCR » ;
 * 2. sinon la catégorie lue par l'OCR (dictionnaire iOS / en-tête), avec badge.
 * « Autre » (valeur Android par défaut quand aucune catégorie n'est choisie) compte comme sans catégorie.
 * Aucun ticket existant n'est lu pour être modifié.
 */
object ScanCategoryResolver {
    data class Suggestion(val category: String, val showBadge: Boolean)

    fun resolve(store: String?, ocr: OCRCategoryGuesser.Guess?, existing: List<Ticket>): Suggestion? {
        val name = store?.trim().orEmpty()
        if (name.isNotEmpty()) {
            val counts = existing
                .filter { it.store.trim().equals(name, ignoreCase = true) }
                .map { it.category.trim() }
                .filter { it.isNotEmpty() && it != TicketFormRules.DEFAULT_CATEGORY }
                .groupingBy { it }.eachCount()
            val best = counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .firstOrNull()
            if (best != null) return Suggestion(best.key, showBadge = best.value < 3)
        }
        return ocr?.let { Suggestion(it.category, showBadge = true) }
    }
}
