package com.etix.ui.add

enum class OCRUiConfidence {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun from(score: Double): OCRUiConfidence =
            when {
                score >= 0.70 -> HIGH
                score >= 0.40 -> MEDIUM
                else -> LOW
            }
    }
}
