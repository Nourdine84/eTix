package com.etix.features.ocr.domain

object OCRConfidence {

    fun compute(
        merchant: String?,
        amount: Double?,
        dateMillis: Long?
    ): Int {

        var score = 0

        if (!merchant.isNullOrBlank()) score += 35
        if (amount != null && amount > 0) score += 45
        if (dateMillis != null) score += 20

        return score.coerceIn(0, 100)
    }
}
