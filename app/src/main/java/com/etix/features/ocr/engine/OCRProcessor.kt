package com.etix.features.ocr.engine

import com.etix.features.ocr.domain.OCRDebug
import com.etix.features.ocr.model.OCRResult
import com.etix.features.ocr.domain.OCRConfidence
import java.util.Locale

object OCRProcessor {

    fun process(rawText: String): OCRResult {

        val lines = rawText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val merchant = extractMerchant(lines)
        val amount = extractBestAmount(lines)
        val dateMillis = OCRDateExtractor.extractDateMillis(lines)

        OCRDebug.d("OCRProcessor", "Merchant = $merchant")
        OCRDebug.d("OCRProcessor", "Amount = $amount")
        OCRDebug.d("OCRProcessor", "DateMillis = $dateMillis")

        val confidence = OCRConfidence.compute(merchant, amount, dateMillis)

        OCRDebug.d("OCRProcessor", "Confidence = $confidence")


        return OCRResult(
            merchant = merchant,
            amount = amount,
            dateMillis = dateMillis,
            rawText = rawText
        )
    }

    // ─────────────────────────────
    // 🏪 MERCHANT
    // ─────────────────────────────
    /**
     * Enseigne : première ligne sans chiffre de 3 à 40 caractères (lot 9 : 3 comme iOS, « KFC », « H&M », « BUT »), en majuscules de préférence.
     * Lot 9 (décision Q1) : une ligne faite uniquement de libellés de montant ou de paiement (« TOTAL »,
     * « TOTAL TTC », « NET A PAYER », « CB »…) ou de politesse (« MERCI DE VOTRE VISITE ») n'est jamais
     * une enseigne. Une enseigne qui contient d'autres mots reste reconnue (« TOTALENERGIES », « TOTAL ENERGIES »).
     */
    internal fun extractMerchant(lines: List<String>): String? {
        val candidates = lines.filter { !isLabelOnly(it) }
        return candidates.firstOrNull {
            it.length in 3..40 &&
                    it == it.uppercase(Locale.getDefault()) &&
                    !it.any(Char::isDigit)
        } ?: candidates.firstOrNull {
            !it.any(Char::isDigit) && it.length in 3..40
        }
    }

    private val LABEL_WORDS = setOf(
        // montant / paiement
        "TOTAL", "TTC", "HT", "NET", "A", "PAYER", "SOUS", "MONTANT", "TVA", "CB", "CARTE", "BANCAIRE",
        "VISA", "MASTERCARD", "ESPECES", "RENDU", "REMISE", "EUR", "EUROS", "SANS", "CONTACT",
        // politesse
        "MERCI", "DE", "VOTRE", "VISITE", "BIENTOT", "AU", "REVOIR", "BIENVENUE", "BONNE", "JOURNEE"
    )

    /** Ligne composée uniquement de libellés (mots de [LABEL_WORDS]), sans aucun autre mot. */
    internal fun isLabelOnly(line: String): Boolean {
        val words = java.text.Normalizer.normalize(line, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "").uppercase(Locale.ROOT)
            .split(Regex("[^A-Z]+")).filter { it.isNotEmpty() }
        return words.isNotEmpty() && words.all { it in LABEL_WORDS }
    }

    // ─────────────────────────────
    // 💰 AMOUNT — OCR NIVEAU 2
    // ─────────────────────────────
    private fun extractBestAmount(lines: List<String>): Double? = OCRAmountExtractor.extract(lines)
}
