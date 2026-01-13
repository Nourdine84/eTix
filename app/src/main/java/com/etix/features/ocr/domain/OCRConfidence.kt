package com.etix.features.ocr.domain

object OCRConfidence {

    fun compute(
        merchant: String?,
        amount: Double?,
        dateMillis: Long?
    ): Double {

        var score = 0.0

        if (!merchant.isNullOrBlank()) score += 0.4
        if (amount != null && amount > 0) score += 0.4
        if (dateMillis != null) score += 0.2

        return score.coerceIn(0.0, 1.0)
    }

    fun level(score: Double): ConfidenceLevel =
        when {
            score >= 0.75 -> ConfidenceLevel.HIGH
            score >= 0.4 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
}

enum class ConfidenceLevel {
    HIGH, MEDIUM, LOW
}
