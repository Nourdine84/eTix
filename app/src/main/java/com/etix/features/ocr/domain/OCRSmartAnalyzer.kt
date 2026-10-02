package com.etix.features.ocr.domain

/**
 * OCRSmartAnalyzer
 *
 * - 100% domain-safe (aucun import Android)
 * - Heuristiques simples mais robustes
 * - Évolutif (ML plus tard si besoin)
 */
object OCRSmartAnalyzer {

    // -----------------------------
    // MODELS
    // -----------------------------

    data class CategoryPrediction(
        val category: String?,
        val confidence: Double
    )

    // -----------------------------
    // PUBLIC API
    // -----------------------------

    /**
     * Catégorie suggérée à partir du texte OCR brut — lot 9 : délègue à OCRCategoryGuesser (catégories iOS,
     * décision Q2). Confiance : enseigne connue 0,80 ; mot d'activité dans l'en-tête 0,50 ; sinon aucune.
     */
    fun guessCategoryWithConfidence(rawText: String): CategoryPrediction {
        val g = com.etix.features.ocr.engine.OCRCategoryGuesser.guess(rawText) ?: return CategoryPrediction(null, 0.0)
        val conf = if (g.source == com.etix.features.ocr.engine.OCRCategoryGuesser.Source.STORE_DICTIONARY) 0.80 else 0.50
        return CategoryPrediction(g.category, conf)
    }

    /**
     * Construit une description courte et propre à partir du texte OCR
     */
    fun buildDescription(rawText: String): String? {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return null

        val ignoreWords = listOf(
            "total", "ttc", "merci", "ticket", "recu", "facture",
            "cb", "visa", "mastercard", "siret", "tva"
        )

        for (line in lines.take(10)) {
            val n = normalize(line)

            var ignored = false
            for (w in ignoreWords) {
                if (n.contains(w)) {
                    ignored = true
                    break
                }
            }
            if (ignored) continue

            // évite les lignes trop numériques
            var letters = 0
            for (c in line) {
                if (c.isLetter()) letters++
            }
            if (letters >= 4) {
                return line.take(60)
            }
        }

        // fallback
        return lines.first().take(60)
    }

    // -----------------------------
    // INTERNALS
    // -----------------------------

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("à", "a")
            .replace("ç", "c")
            .replace("ô", "o")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
