package com.etix.features.ocr.engine

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object OCRDateExtractor {

    private val formats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy")
    )

    fun extract(lines: List<String>): Long? {

        for (line in lines) {
            for (formatter in formats) {
                try {
                    val date = LocalDate.parse(line, formatter)
                    return date
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
