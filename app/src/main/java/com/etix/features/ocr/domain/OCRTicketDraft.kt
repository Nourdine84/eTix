package com.etix.features.ocr.domain

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import com.etix.features.ocr.domain.OCRSmartAnalyzer


/**
 * TicketDraft = données OCR prêtes à pré-remplir un formulaire.
 * Aucun import Android, 100% domain-safe.
 */
data class TicketDraft(
    val storeName: String? = null,
    val amount: Double? = null,
    val dateMillis: Long? = null,
    val category: String? = null,
    val description: String? = null,
    val confidence: Double = 0.0  // Changé de OCRConfidence à Double
)

object OCRTicketDraftMapper {

    private val locale = Locale.getDefault()

    // formats courants FR
    private val dateFormats = listOf(
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "dd.MM.yyyy",
        "dd/MM/yy",
        "dd-MM-yy",
        "dd.MM.yy"
    )

    /**
     * Map OCR raw text -> TicketDraft
     */
    fun toDraft(rawText: String): TicketDraft {
        val cleanText = rawText.trim()
        if (cleanText.isEmpty()) return TicketDraft()

        val normalized = normalizeForSearch(cleanText)

        val store = extractStoreName(cleanText)
        val amount = extractAmount(cleanText)
        val dateMillis = extractDateMillis(cleanText)

        val prediction = OCRSmartAnalyzer.guessCategoryWithConfidence(cleanText)
        val description = OCRSmartAnalyzer.buildDescription(cleanText)

        // Confiance globale : on prend la confiance catégorie comme base
        val conf = prediction.confidence

        return TicketDraft(
            storeName = store,
            amount = amount,
            dateMillis = dateMillis,
            category = prediction.category,
            description = description,
            confidence = conf
        )
    }

    // -----------------------------
    // STORE
    // -----------------------------
    private fun extractStoreName(rawText: String): String? {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return null

        // On ignore les lignes trop techniques
        val ignoreWords = listOf(
            "ticket", "recu", "facture", "siret", "tva", "ttc", "cb",
            "visa", "mastercard", "autorisation", "merci", "total"
        )

        for (line in lines.take(8)) { // on scanne le haut du ticket
            val n = normalizeForSearch(line)

            var ignored = false
            for (w in ignoreWords) {
                if (n.contains(w)) {
                    ignored = true
                    break
                }
            }
            if (ignored) continue

            // évite une ligne quasi numérique
            var digits = 0
            var letters = 0
            for (c in line) {
                if (c.isDigit()) digits++
                if (c.isLetter()) letters++
            }
            if (letters >= 3 && digits <= 6) {
                return line.take(40)
            }
        }

        // fallback
        return lines.first().take(40)
    }

    // -----------------------------
    // AMOUNT
    // -----------------------------
    // Lot 6 : même règle que OCRProcessor (sous-total / HT / TVA / remise / rendu jamais retenus)
    private fun extractAmount(rawText: String): Double? =
        com.etix.features.ocr.engine.OCRAmountExtractor.extract(rawText.lines())

    private fun parseAmount(s: String): Double? {
        // "1 234,56" -> "1234.56"
        val cleaned = s.replace(" ", "").replace(".", "X") // protège les milliers
        val normalized = cleaned.replace(",", ".").replace("X", "")
        return normalized.toDoubleOrNull()
    }

    private fun round2(v: Double): Double {
        val x = (v * 100.0)
        val r = if (x >= 0) (x + 0.5).toLong() else (x - 0.5).toLong()
        return r / 100.0
    }

    // -----------------------------
    // DATE
    // -----------------------------
    private fun extractDateMillis(rawText: String): Long? {
        // cherche un pattern date
        val dateRegex = Regex("""\b(\d{2}[\/\-.]\d{2}[\/\-.]\d{2,4})\b""")
        val match = dateRegex.find(rawText) ?: return null
        val rawDate = match.value

        for (fmt in dateFormats) {
            val sdf = SimpleDateFormat(fmt, locale)
            sdf.isLenient = true
            val d = try { sdf.parse(rawDate) } catch (_: Exception) { null }
            if (d != null) {
                // sanity: évite une date trop loin (si OCR a mal lu)
                val now = System.currentTimeMillis()
                val diffDays = abs(now - d.time) / (1000L * 60L * 60L * 24L)
                if (diffDays <= 3650) return d.time // <= 10 ans
            }
        }

        return null
    }

    // -----------------------------
    // HELPERS
    // -----------------------------
    private fun normalizeForSearch(s: String): String {
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

    private fun containsAny(text: String, words: List<String>): Boolean {
        for (w in words) {
            if (text.contains(w)) return true
        }
        return false
    }
}