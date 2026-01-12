package com.etix.features.ocr.validation

object OCRValidationUtils {

    fun loadText(fileName: String): String {
        return requireNotNull(
            OCRValidationUtils::class.java.classLoader
                ?.getResource("ocr/$fileName")
        ) {
            "Fichier OCR introuvable : $fileName"
        }.readText()
    }
}
