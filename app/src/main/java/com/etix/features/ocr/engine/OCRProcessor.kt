package com.etix.features.ocr.engine

import com.etix.features.ocr.domain.OCRResult

class OCRProcessor {

    fun process(imageBytes: ByteArray): OCRResult {
        // 🔧 STUB – implémentation réelle en V2.01
        return OCRResult(
            storeName = "TEST OCR",
            amount = 0.0,
            dateMillis = System.currentTimeMillis()
        )
    }
}
