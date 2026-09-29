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
    private fun extractBestAmount(lines: List<String>): Double? = OCRAmountExtractor.extract(lines)
}
