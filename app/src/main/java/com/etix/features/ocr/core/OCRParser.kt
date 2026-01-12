package com.etix.features.ocr.core

import com.etix.features.ocr.engine.OCRProcessor
import com.etix.features.ocr.model.OCRResult

object OCRParser {

    fun parse(rawText: String): OCRResult {
        // ✅ Source de vérité : OCRProcessor
        return OCRProcessor.process(rawText)
    }
}
