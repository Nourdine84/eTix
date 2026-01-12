package com.etix.features.ocr.domain

import com.etix.features.ocr.model.OCRResult

object OCRMapper {

    fun map(rawText: String): OCRResult {
        // 🔥 Version simple ISO V2.01
        // L’OCR intelligent est dans OCRProcessor

        val merchant = extractMerchant(rawText)
        val amount = extractAmount(rawText)

        return OCRResult(
            merchant = merchant,
            amount = amount,
            dateMillis = null,
            rawText = rawText
        )
    }

    private fun extractMerchant(text: String): String? {
        return text
            .lines()
            .firstOrNull()
            ?.take(40)
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("""(\d+[.,]\d{2})""")
        val match = regex.find(text)?.value ?: return null
        return match.replace(",", ".").toDoubleOrNull()
    }
}
