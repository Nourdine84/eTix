package com.etix.features.ocr.domain

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/**
 * OCR Smart Analyzer v2 (BUILD SAFE)
 */
object OCRSmartAnalyzer {

    data class CategoryPrediction(
        val category: String,
        val confidence: Double
    )

    private data class Rule(
        val category: String,
        val keywords: List<String>,
        val strongKeywords: List<String> = emptyList()
    )

    private val rules = listOf(
        Rule(
            "Supermarché",
            listOf("carrefour","auchan","leclerc","intermarche","casino","lidl","aldi","monoprix"),
            listOf("carrefour","leclerc","auchan","intermarche","monoprix")
        ),
        Rule(
            "Restaurant",
            listOf("restaurant","brasserie","pizzeria","kebab","burger","sandwich","boulangerie","cafe","bar"),
            listOf("restaurant","brasserie","pizzeria","kebab")
        ),
        Rule(
            "Transport",
            listOf("sncf","ratp","uber","taxi","station","essence","carburant","diesel","gazole","peage","parking"),
            listOf("sncf","ratp","uber","carburant","essence")
        ),
        Rule(
            "Santé",
            listOf("pharmacie","medecin","hopital","clinique","dentiste","optique"),
            listOf("pharmacie","hopital","clinique","dentiste")
        ),
        Rule(
            "Shopping",
            listOf("zara","hm","kiabi","primark","decathlon","fnac","darty","boulanger","ikea","amazon"),
            listOf("decathlon","fnac","darty","boulanger","ikea","amazon")
        ),
        Rule(
            "Maison",
            listOf("leroy","merlin","castorama","brico","outil","peinture","plomberie"),
            listOf("leroy","merlin","castorama")
        )
    )

    fun guessCategory(rawText: String): String =
        guessCategoryWithConfidence(rawText).category

    fun guessCategoryWithConfidence(rawText: String): CategoryPrediction {
        val text = normalize(rawText)

        var bestCategory = "Autre"
        var bestScore = 0.0
        var bestReasons = emptyList<String>()

        for (rule in rules) {
            val result = scoreRule(text, rule)
            if (result.first > bestScore) {
                bestScore = result.first
                bestCategory = rule.category
                bestReasons = result.second
            }
        }

        // Utilise directement le score comme confiance
        val confidence = bestScore

        return CategoryPrediction(bestCategory, confidence)
    }

    fun buildDescription(rawText: String): String {
        val lines = rawText.split("\n")
        val ignoreWords = listOf(
            "total","ttc","tva","cb","visa","mastercard",
            "ticket","recu","facture","siret","tpe","merci"
        )

        val useful = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val norm = normalize(trimmed)

            var ignored = false
            for (word in ignoreWords) {
                if (norm.contains(word)) {
                    ignored = true
                    break
                }
            }
            if (ignored) continue

            var numeric = true
            for (c in trimmed) {
                if (!c.isDigit()) {
                    numeric = false
                    break
                }
            }
            if (numeric && trimmed.length <= 6) continue

            useful.add(trimmed)
            if (useful.size >= 2) break
        }

        return useful.joinToString(" • ").take(90)
    }

    private fun scoreRule(text: String, rule: Rule): Pair<Double, List<String>> {
        var score = 0.0
        val reasons = mutableListOf<String>()

        for (kw in rule.strongKeywords) {
            if (text.contains(kw)) {
                score += 0.35
                reasons.add("strong:$kw")
            }
        }

        var hits = 0
        for (kw in rule.keywords) {
            if (text.contains(kw)) hits++
        }

        if (hits > 0) {
            score += min(0.5, hits * 0.12)
            reasons.add("hits=$hits")
        }

        return clip(score) to reasons
    }

    private fun normalize(s: String): String {
        val lower = s.lowercase()
        val noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccent
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun clip(v: Double): Double =
        max(0.0, min(1.0, v))
}