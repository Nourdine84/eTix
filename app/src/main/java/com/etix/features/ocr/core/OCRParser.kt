package com.etix.features.ocr.core

import com.etix.features.ocr.domain.OCRResult

object OCRParser {

    fun parse(rawText: String): OCRResult {
        return OCRResult(
            merchant = null,
            amount = null,
            dateMillis = null,
            rawText = rawText
        )
    }
}
