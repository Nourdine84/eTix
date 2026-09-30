package com.etix.features.ocr.engine

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Extraction date v2:
 * - Supporte dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy
 * - Supporte aussi dd/MM/yy etc.
 * Retourne dateMillis (startOfDay).
 */
/** Implémentation d'origine (java.time, avant le lot 8 durcissement), conservée en test comme référence d'équivalence. */
internal object OCRDateExtractorJavaTimeReference {

    private val patterns = listOf(
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "dd.MM.yyyy",
        "dd/MM/yy",
        "dd-MM-yy",
        "dd.MM.yy"
    )

    fun extractDateMillis(lines: List<String>): Long? {
        val cleaned = lines
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (line in cleaned) {
            val candidate = line.replace(" ", "")
            val parsed = tryParse(candidate) ?: continue
            return parsed
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        return null
    }

    private fun tryParse(text: String): LocalDate? {
        for (p in patterns) {
            try {
                val formatter = DateTimeFormatter.ofPattern(p, Locale.getDefault())
                return LocalDate.parse(text, formatter)
            } catch (_: Exception) {
                // ignore
            }
        }

        // fallback : détecter une date au milieu d'une ligne (ex "DATE: 12/01/2026")
        val regex = Regex("""(\d{2}[./-]\d{2}[./-]\d{2,4})""")
        val m = regex.find(text)?.groupValues?.getOrNull(1) ?: return null
        val normalized = m.replace('-', '/').replace('.', '/')

        // essayer avec 2 formats
        val tryFormats = listOf("dd/MM/yyyy", "dd/MM/yy")
        for (p in tryFormats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(p, Locale.getDefault())
                return LocalDate.parse(normalized, formatter)
            } catch (_: Exception) {
            }
        }
        return null
    }
}
