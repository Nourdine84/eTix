package com.etix.features.ocr.engine

import com.etix.features.ocr.domain.OCRConfidence
import com.etix.features.ocr.domain.OCRDebug
import com.etix.features.ocr.model.OCRResult
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
        val dateMillis = OCRDateExtractor.extract(lines)

        OCRDebug.log("Merchant: $merchant")
        OCRDebug.log("Amount: $amount")
        OCRDebug.log("DateMillis: $dateMillis")
        OCRDebug.log("Confidence: ${
            OCRConfidence.compute(merchant, amount, dateMillis)
        }")

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
    // 💰 AMOUNT — NIVEAU 2
    // ─────────────────────────────
    private fun extractBestAmount(lines: List<String>): Double? {

        val candidates = mutableListOf<Double>()
        val regex = Pattern.compile("(\\d+[,.]\\d{2})")

        for (line in lines) {

            if (line.contains("TOTAL", true) || line.contains("TTC", true)) {
                regex.matcher(line.replace(" ", "")).let {
                    if (it.find()) {
                        return it.group(1)
                            ?.replace(",", ".")
                            ?.toDoubleOrNull()
                    }
                }
            }

            regex.matcher(line.replace(" ", "")).let {
                if (it.find()) {
                    it.group(1)
                        ?.replace(",", ".")
                        ?.toDoubleOrNull()
                        ?.takeIf { v -> v > 0.5 }
                        ?.let { v -> candidates.add(v) }
                }
            }
        }

        return candidates.maxOrNull()
    }
}
