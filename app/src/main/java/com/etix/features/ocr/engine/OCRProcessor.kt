package com.etix.features.ocr.engine

import com.etix.features.ocr.domain.OCRResult
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

object OCRProcessor {

    fun process(rawText: String): OCRResult {

        val lines = rawText
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val merchant = extractMerchant(lines)
        val amount = extractAmount(lines)
        val dateMillis = extractDateMillis(lines)

        return OCRResult(
            merchant = merchant,
            amount = amount,
            dateMillis = dateMillis,
            rawText = rawText
        )
    }

    // -----------------------------
    // 💰 MONTANT
    // -----------------------------
    private fun extractAmount(lines: List<String>): Double? {
        val regex = Pattern.compile(
            "(\\d{1,3}[,.]\\d{2})\\s?(€|eur)?",
            Pattern.CASE_INSENSITIVE
        )

        for (line in lines.reversed()) {
            val matcher = regex.matcher(line.replace(" ", ""))
            if (matcher.find()) {
                return matcher.group(1)
                    ?.replace(",", ".")
                    ?.toDoubleOrNull()
            }
        }
        return null
    }

    // -----------------------------
    // 🏪 COMMERÇANT
    // -----------------------------
    private fun extractMerchant(lines: List<String>): String? {
        return lines.firstOrNull {
            it.length >= 3 &&
                    it == it.uppercase(Locale.getDefault()) &&
                    !it.any(Char::isDigit)
        } ?: lines.firstOrNull { !it.any(Char::isDigit) }
    }

    // -----------------------------
    // 📅 DATE → Long (millis)
    // -----------------------------
    private fun extractDateMillis(lines: List<String>): Long? {
        val formats = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd.MM.yyyy"
        )

        for (line in lines) {
            for (pattern in formats) {
                try {
                    val localDate = LocalDate.parse(
                        line,
                        DateTimeFormatter.ofPattern(pattern)
                    )
                    return localDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                }
            }
        }
        return null
    }
}
