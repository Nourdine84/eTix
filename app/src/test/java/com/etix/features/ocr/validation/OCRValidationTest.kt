package com.etix.features.ocr.validation

import com.etix.features.ocr.engine.OCRProcessor
import com.etix.features.ocr.domain.OCRSmartAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class OCRValidationTest {

    private val datasets = listOf(
        OCRDataset(
            fileName = "ticket_carrefour.txt",
            expected = OCRExpected(
                merchant = "CARREFOUR",
                amount = 23.45,
                category = "Supermarché"
            )
        ),
        OCRDataset(
            fileName = "ticket_restaurant.txt",
            expected = OCRExpected(
                merchant = "RESTAURANT",
                amount = 18.90,
                category = "Restaurant"
            )
        )
    )

    // Désactivé (lot 1) : les datasets référencent ticket_carrefour.txt / ticket_restaurant.txt
    // absents de src/test/resources/ocr (ticket_001..004), et les attentes ne correspondent pas
    // aux fixtures présentes. À réaligner avec le lot OCR — voir docs/SUIVI_ANDROID.md.
    @Ignore("Datasets OCR désalignés avec les fixtures — à reprendre au lot OCR")
    @Test
    fun validate_ocr_results() {
        datasets.forEach { dataset ->

            val rawText = OCRValidationUtils.loadText(dataset.fileName)
            val result = OCRProcessor.process(rawText)

            assertEquals(
                dataset.expected.merchant,
                result.merchant?.uppercase()
            )

            assertEquals(
                dataset.expected.amount,
                result.amount
            )

            val category = OCRSmartAnalyzer.guessCategoryWithConfidence(rawText).category
            assertEquals(
                dataset.expected.category,
                category
            )
        }
    }
}
