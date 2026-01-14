package com.etix.features.ocr.domain

import kotlin.math.min

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
     * Déduit une catégorie + un niveau de confiance à partir du texte OCR brut
     */
    fun guessCategoryWithConfidence(rawText: String): CategoryPrediction {
        val text = normalize(rawText)
        if (text.isEmpty()) {
            return CategoryPrediction(null, 0.0)
        }

        val scores = mutableMapOf<String, Int>()

        for ((category, keywords) in CATEGORY_KEYWORDS) {
            var score = 0
            for (word in keywords) {
                if (text.contains(word)) {
                    score += 1
                }
            }
            if (score > 0) {
                scores[category] = score
            }
        }

        if (scores.isEmpty()) {
            return CategoryPrediction(null, 0.0)
        }

        val best = scores.maxByOrNull { it.value }!!
        val confidence = computeConfidence(best.value)

        return CategoryPrediction(
            category = best.key,
            confidence = confidence
        )
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

    private fun computeConfidence(score: Int): Double {
        // Heuristique simple :
        // 1 mot clé → ~0.45
        // 2 → ~0.65
        // 3 → ~0.80
        // 4+ → ~0.90+
        return min(0.95, 0.35 + score * 0.15)
    }

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

    // -----------------------------
    // CATEGORY DICTIONARY
    // -----------------------------

    private val CATEGORY_KEYWORDS = mapOf(

        "Alimentation" to listOf(
            "carrefour", "leclerc", "auchan", "lidl", "aldi", "intermarche",
            "supermarche", "hypermarche", "casino", "monoprix",
            "boulangerie", "boucherie", "epicerie"
        ),

        "Restaurant" to listOf(
            "restaurant", "brasserie", "cafe", "bar", "pizza",
            "kebab", "snack", "fast food", "mcdo", "mcdonald",
            "burger", "tacos"
        ),

        "Transport" to listOf(
            "sncf", "ratp", "uber", "bolt", "taxi",
            "peage", "autoroute", "essence", "carburant",
            "station", "parking"
        ),

        "Santé" to listOf(
            "pharmacie", "pharma", "docteur", "medecin",
            "hopital", "clinique", "dentiste"
        ),

        "Shopping" to listOf(
            "zara", "hm", "h&m", "uniqlo", "celio",
            "fnac", "darty", "boulanger",
            "amazon", "ikea"
        ),

        "Loisirs" to listOf(
            "cinema", "theatre", "concert", "netflix",
            "spotify", "disney", "abonnement"
        )
    )
}
