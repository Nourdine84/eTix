package com.etix.features.ocr.validation

import com.etix.features.ocr.domain.OCRSmartAnalyzer
import com.etix.features.ocr.engine.OCRProcessor
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OBSERVATION, pas validation : exécute le moteur OCR réel sur les 4 fixtures présentes et
 * consigne ce qu'il produit (build/ocr-report.txt, publié par la CI).
 * Aucune assertion (les attentes sont vérifiées par OCRFixturesTest, décisions Q1–Q4 validées au lot 9).
 * OCRValidationTest (obsolète) reste désactivé.
 */
class OCRFixtureObservationTest {

    @Test
    fun observe_existing_fixtures() {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val lines = listOf("ticket_001.txt", "ticket_002.txt", "ticket_003.txt", "ticket_004.txt").map { f ->
            val raw = OCRValidationUtils.loadText(f)
            val r = OCRProcessor.process(raw)
            val c = OCRSmartAnalyzer.guessCategoryWithConfidence(raw)
            "$f | enseigne=${r.merchant} | montant=${r.amount} | date=${r.dateMillis?.let { fmt.format(Date(it)) }} " +
                "| catégorie=${c.category} | confiance=${"%.2f".format(Locale.US, c.confidence)}"
        }
        val all = lines
        File("build").mkdirs()
        File("build/ocr-report.txt").writeText(all.joinToString("\n"))
        all.forEach(::println)
    }
}
