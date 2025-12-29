package com.etix.features.ocr.engine

import com.etix.features.ocr.domain.OCRMapper
import com.etix.features.ocr.domain.OCRResult

object OCRProcessor {

    fun process(text: String): OCRResult {
        return OCRMapper.map(text)
    }
}
