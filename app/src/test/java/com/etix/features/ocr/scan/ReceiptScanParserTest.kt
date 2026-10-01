package com.etix.features.ocr.scan

import com.etix.features.ocr.engine.OCRCategoryGuesser
import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Lot 9 — texte reconnu → champs du formulaire, confiances iOS (ReceiptParser), catégorie (StoreCategoryMapper). */
class ReceiptScanParserTest {

    @Test fun ticket_esso_complet() {
        val s = ReceiptScanParser.parse("ESSO\n12/01/2026\nTOTAL TTC 23,45 €\nCB VISA\nMERCI DE VOTRE VISITE")
        assertEquals("ESSO", s.store.value); assertEquals(ScanConfidence.MEDIUM, s.store.confidence)
        assertEquals(23.45, s.amount.value!!, 0.001); assertEquals(ScanConfidence.HIGH, s.amount.confidence)
        val c = Calendar.getInstance().apply { timeInMillis = s.date.value!! }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
        assertEquals(ScanConfidence.HIGH, s.date.confidence)
        assertEquals("Carburant", s.category?.category)
        assertFalse(s.isEmpty)
    }

    @Test fun montant_hors_ligne_de_total_a_verifier() {
        val s = ReceiptScanParser.parse("BOULANGERIE MARTIN\nBAGUETTE 1,20\nCB 4,20")
        assertEquals(4.20, s.amount.value!!, 0.001)
        assertEquals(ScanConfidence.LOW, s.amount.confidence)
        assertNull(s.date.value); assertEquals(ScanConfidence.NONE, s.date.confidence)
    }

    @Test fun texte_vide_ou_illisible_rien_detecte() {
        assertTrue(ReceiptScanParser.parse("").isEmpty)
        assertTrue(ReceiptScanParser.parse("  \n \n").isEmpty)
        assertTrue(ReceiptScanParser.parse("TOTAL\nMERCI").isEmpty)
    }

    @Test fun total_seul_montant_sans_enseigne() {
        val s = ReceiptScanParser.parse("TOTAL\nTOTAL 58,20")
        assertNull(s.store.value); assertNull(s.category)
        assertEquals(58.20, s.amount.value!!, 0.001)
        assertFalse(s.isEmpty)
    }

    // ---------- catégorie : historique du magasin (lecture seule) puis OCR ----------
    private fun t(store: String, cat: String) = Ticket(store = store, amount = 1.0, category = cat, dateMillis = 0)
    private val essoOcr = OCRCategoryGuesser.Guess("Carburant", OCRCategoryGuesser.Source.STORE_DICTIONARY)

    @Test fun sans_historique_categorie_ocr_avec_badge() {
        assertEquals(ScanCategoryResolver.Suggestion("Carburant", true), ScanCategoryResolver.resolve("ESSO", essoOcr, emptyList()))
    }

    @Test fun historique_faible_prioritaire_avec_badge() {
        assertEquals(ScanCategoryResolver.Suggestion("Transport", true),
            ScanCategoryResolver.resolve("ESSO", essoOcr, listOf(t("Esso", "Transport"))))
    }

    @Test fun historique_fort_sans_badge() {
        val h = List(3) { t("esso ", "Transport") } + t("ESSO", "Carburant")
        assertEquals(ScanCategoryResolver.Suggestion("Transport", false), ScanCategoryResolver.resolve("ESSO", essoOcr, h))
    }

    @Test fun autre_et_vide_ne_comptent_pas_comme_historique() {
        assertEquals(ScanCategoryResolver.Suggestion("Carburant", true),
            ScanCategoryResolver.resolve("ESSO", essoOcr, listOf(t("ESSO", "Autre"), t("ESSO", " "))))
    }

    @Test fun ni_historique_ni_ocr_aucune_suggestion() {
        assertNull(ScanCategoryResolver.resolve("MAISON DUPONT", null, emptyList()))
        assertNull(ScanCategoryResolver.resolve(null, null, listOf(t("ESSO", "Transport"))))
    }
}
