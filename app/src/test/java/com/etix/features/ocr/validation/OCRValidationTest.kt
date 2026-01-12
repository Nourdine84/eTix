package com.etix.features.ocr.validation

import com.etix.features.ocr.engine.OCRProcessor
import com.etix.features.ocr.domain.OCRSmartAnalyzer
import org.junit.Assert.assertEquals
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

            val category = OCRSmartAnalyzer.guessCategory(rawText)
            assertEquals(
                dataset.expected.category,
                category
            )
        }
    }
}
