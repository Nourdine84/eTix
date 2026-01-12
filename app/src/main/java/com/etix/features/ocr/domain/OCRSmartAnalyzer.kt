package com.etix.features.ocr.domain

import java.util.Locale

object OCRSmartAnalyzer {

    fun guessCategory(rawText: String): String {
        val text = rawText.lowercase(Locale.getDefault())

        return when {
            text.contains("carrefour") ||
                    text.contains("lidl") ||
                    text.contains("intermarch") ||
                    text.contains("auchan") ->
                "Supermarché"

            text.contains("uber") ||
                    text.contains("bolt") ||
                    text.contains("sncf") ||
                    text.contains("ratp") ->
                "Transport"

            text.contains("pharm") ||
                    text.contains("docteur") ||
                    text.contains("optique") ->
                "Santé"

            text.contains("restaurant") ||
                    text.contains("pizza") ||
                    text.contains("kebab") ||
                    text.contains("mcdo") ->
                "Restaurant"

            text.contains("amazon") ||
                    text.contains("fnac") ||
                    text.contains("darty") ->
                "Shopping"

            else -> "Autre"
        }
    }

    fun buildDescription(rawText: String): String {
        return rawText
            .lines()
            .firstOrNull { it.length in 5..40 }
            ?: "Ticket OCR"
    }
}
