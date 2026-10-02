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

    /** « Maintenant » fixe (02/10/2026 midi) : confiance des dates indépendante du jour d'exécution. */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 2, 12, 0) }.timeInMillis
    private fun parse(t: String) = ReceiptScanParser.parse(t, now)

    @Test fun ticket_esso_complet() {
        val s = parse("ESSO\n12/01/2026\nTOTAL TTC 23,45 €\nCB VISA\nMERCI DE VOTRE VISITE")
        assertEquals("ESSO", s.store.value); assertEquals(ScanConfidence.MEDIUM, s.store.confidence)
        assertEquals(23.45, s.amount.value!!, 0.001); assertEquals(ScanConfidence.HIGH, s.amount.confidence)
        val c = Calendar.getInstance().apply { timeInMillis = s.date.value!! }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
        assertEquals(ScanConfidence.HIGH, s.date.confidence)
        assertEquals("Carburant", s.category?.category)
        assertFalse(s.isEmpty)
    }

    @Test fun montant_hors_ligne_de_total_a_verifier() {
        val s = parse("BOULANGERIE MARTIN\nBAGUETTE 1,20\nCB 4,20")
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
        val s = parse("TOTAL\nTOTAL 58,20")
        assertNull(s.store.value); assertNull(s.category)
        assertEquals(58.20, s.amount.value!!, 0.001)
        assertFalse(s.isEmpty)
    }

    // ---------- valeurs ambiguës ou peu plausibles : valeur inchangée, confiance basse (« À vérifier ») ----------

    @Test fun deux_totaux_differents_montant_a_verifier() {
        val s = parse("ESSO\n12/09/2026\nTOTAL 23,45\nTOTAL 32,45\nCB")
        assertEquals(32.45, s.amount.value!!, 0.001)                 // règle lot 6 inchangée : dernière ligne
        assertEquals(ScanConfidence.LOW, s.amount.confidence)
    }

    @Test fun meme_total_repete_reste_detecte() {
        val s = parse("ESSO\n12/09/2026\nTOTAL 23,45\nTOTAL 23,45")
        assertEquals(ScanConfidence.HIGH, s.amount.confidence)
    }

    @Test fun plusieurs_dates_differentes_date_a_verifier() {
        val s = parse("CARREFOUR\n12/09/2026\nTOTAL 10,00\nBON VALABLE JUSQU'AU 31/12/2026")
        val c = Calendar.getInstance().apply { timeInMillis = s.date.value!! }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.SEPTEMBER, c.get(Calendar.MONTH))
        assertEquals(ScanConfidence.LOW, s.date.confidence)
    }

    @Test fun date_future_a_verifier() {
        assertEquals(ScanConfidence.LOW, parse("ESSO\n15/11/2026\nTOTAL 10,00").date.confidence)
        assertEquals(ScanConfidence.HIGH, parse("ESSO\n03/10/2026\nTOTAL 10,00").date.confidence)  // demain : décalage horaire toléré
    }

    @Test fun date_tres_ancienne_a_verifier() {
        assertEquals(ScanConfidence.LOW, parse("ESSO\n12/01/2020\nTOTAL 10,00").date.confidence)
        assertEquals(ScanConfidence.HIGH, parse("ESSO\n03/10/2024\nTOTAL 10,00").date.confidence)
    }

    /** Champ absent : aucune valeur, aucune confiance (affiché « Non lu »), jamais de date du jour par défaut. */
    @Test fun champs_absents_sans_valeur_par_defaut() {
        val s = parse("MAGASIN DUPONT\nMERCI")
        assertNull(s.date.value); assertEquals(ScanConfidence.NONE, s.date.confidence)
        assertNull(s.amount.value); assertEquals(ScanConfidence.NONE, s.amount.confidence)
    }

    // ---------- image : budget de pixels ----------

    @Test fun sous_echantillonnage_garde_au_moins_le_budget() {
        val max = ScanImageLoader.MAX_PIXELS
        assertEquals(1, ScanImageLoader.sampleSize(1080, 1500, max))
        assertEquals(1, ScanImageLoader.sampleSize(4000, 3000, max))      // 12 Mpx → 3 Mpx avec 2 : trop petit
        assertEquals(2, ScanImageLoader.sampleSize(6000, 8000, max))      // 48 Mpx → 12 Mpx, puis échelle exacte
        assertEquals(1, ScanImageLoader.sampleSize(1000, 8000, max))      // ticket long : largeur préservée
        assertEquals(4, ScanImageLoader.sampleSize(12000, 9000, max))
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
