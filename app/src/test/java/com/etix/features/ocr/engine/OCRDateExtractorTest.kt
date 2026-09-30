package com.etix.features.ocr.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Caractérisation des règles actuelles du lecteur de dates OCR (aucune règle modifiée).
 * Sert de référence au test sur appareil E2eCompatDatesOcrTest (API 21 à 36) : les deux
 * doivent donner les mêmes résultats, avec ou sans désucrage de java.time.
 */
class OCRDateExtractorTest {

    private fun day(y: Int, m: Int, d: Int): Long = Calendar.getInstance().apply {
        clear(); set(y, m - 1, d, 0, 0, 0)
    }.timeInMillis

    private fun check(expected: Long?, vararg lines: String) =
        assertEquals(lines.toList().toString(), expected, OCRDateExtractor.extractDateMillis(lines.toList()))

    @Test fun formats_acceptes() {
        check(day(2026, 1, 12), "CARREFOUR", "12/01/2026", "TOTAL 10,00")
        check(day(2026, 9, 15), "DATE: 15-09-2026 14:32")
        check(day(2026, 9, 15), "15.09.26")
        check(day(2026, 1, 12), "12 / 01 / 2026")
        check(day(2099, 1, 1), "01/01/99") // année sur 2 chiffres : 2000 à 2099
    }

    @Test fun premiere_ligne_datee_retenue() {
        check(day(2026, 10, 3), "TEL 01.23.45.67.89", "03/10/2026")
        check(day(2026, 1, 12), "12/01/2026", "13/01/2026")
    }

    @Test fun jours_hors_mois_ramenes_au_dernier_jour() {
        check(day(2024, 2, 29), "29/02/2024")
        check(day(2025, 2, 28), "29/02/2025")
        check(day(2026, 2, 28), "31/02/2026")
    }

    @Test fun rejets() {
        check(null, "32/01/2026")
        check(null, "12/13/2026")
        check(null, "1/2/2026")
        check(null, "12/01/202")
        check(null, "pas de date")
        check(null)
    }

    @Test fun annee_trop_longue_lue_par_le_repli() {
        check(day(2026, 1, 12), "12/01/20261")
    }
}
