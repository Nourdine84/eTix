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
 * Aucune assertion : les attentes restent à valider par le produit (docs/OCR_CAS_DE_REFERENCE.md).
 * OCRValidationTest reste désactivé tant que ces attentes ne sont pas validées.
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
        // Cas désactivés : ce que donnerait l'attente PROPOSÉE aujourd'hui (aucun échec levé, observation seule)
        fun res(f: String) = OCRProcessor.process(OCRValidationUtils.loadText(f))
        fun cat(f: String) = OCRSmartAnalyzer.guessCategoryWithConfidence(OCRValidationUtils.loadText(f)).category
        fun day(f: String) = res(f).dateMillis?.let { fmt.format(Date(it)) }
        val blocked = listOf(
            Triple("t001_categorie (Q2)", "Alimentation", cat("ticket_001.txt")),
            Triple("t002_categorie (Q2)", "Alimentation", cat("ticket_002.txt")),
            Triple("t003_enseigne (Q1)", null, res("ticket_003.txt").merchant),
            Triple("t003_categorie (Q1/Q2)", null, cat("ticket_003.txt")),
            Triple("t004_date (Q3/Q4)", "12/01/2026", day("ticket_004.txt")),
            Triple("t004_categorie (Q2)", "Carburant", cat("ticket_004.txt")),
            Triple("s_restaurant_categorie (Q2)", "Restaurant", cat("synthetique/restaurant_synthetique.txt")),
            Triple("s_long_date (Q4)", "15/09/2026", day("synthetique/ticket_long_synthetique.txt")),
            Triple("s_long_categorie (Q2)", "Alimentation", cat("synthetique/ticket_long_synthetique.txt")),
        ).map { (name, expected, actual) ->
            "désactivé $name | attendu=$expected | moteur=$actual | " +
                (if (expected == actual) "PASSERAIT si la proposition est validée" else "ÉCHOUERAIT : défaut moteur à corriger")
        }
        val all = lines + blocked
        File("build").mkdirs()
        File("build/ocr-report.txt").writeText(all.joinToString("\n"))
        all.forEach(::println)
    }
}
