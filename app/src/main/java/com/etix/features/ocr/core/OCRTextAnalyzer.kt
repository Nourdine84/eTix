package com.etix.features.ocr.core

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicBoolean

class OCRTextAnalyzer(
    private val onTextDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val hasResult = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        if (hasResult.get()) {
            image.close()
            return
        }

        // ⚠️ OCR réel à brancher plus tard (MLKit)
        // Pour l’instant, extraction brute simulée propre
        val fakeText = """
            CARREFOUR
            TOTAL 25.99 €
            15/03/2024
        """.trimIndent()

        hasResult.set(true)
        onTextDetected(fakeText)

        image.close()
    }
}
