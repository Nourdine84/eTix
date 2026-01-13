package com.etix.features.ocr.engine

import com.etix.features.ocr.domain.OCRDebug
import com.etix.features.ocr.model.OCRResult
import com.etix.features.ocr.domain.OCRConfidence
import java.util.Locale
import java.util.regex.Pattern

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
    private fun extractMerchant(lines: List<String>): String? {
        return lines.firstOrNull {
            it.length in 4..40 &&
                    it == it.uppercase(Locale.getDefault()) &&
                    !it.any(Char::isDigit)
        } ?: lines.firstOrNull {
            !it.any(Char::isDigit) && it.length in 4..40
        }
    }

    // ─────────────────────────────
    // 💰 AMOUNT — OCR NIVEAU 2
    // ─────────────────────────────
    private fun extractBestAmount(lines: List<String>): Double? {

        val candidates = mutableListOf<Double>()
        val regex = Pattern.compile("(\\d+[,.]\\d{2})")

        for (line in lines) {

            // priorité TOTAL / TTC
            if (line.contains("TOTAL", true) || line.contains("TTC", true)) {
                val m = regex.matcher(line.replace(" ", ""))
                if (m.find()) {
                    return m.group(1)
                        ?.replace(",", ".")
                        ?.toDoubleOrNull()
                }
            }

            // fallback : toutes les valeurs valides
            val m = regex.matcher(line.replace(" ", ""))
            if (m.find()) {
                m.group(1)
                    ?.replace(",", ".")
                    ?.toDoubleOrNull()
                    ?.takeIf { it > 0.5 }
                    ?.let { candidates.add(it) }
            }
        }

        return candidates.maxOrNull()
    }
}
