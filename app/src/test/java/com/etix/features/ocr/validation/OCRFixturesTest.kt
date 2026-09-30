package com.etix.features.ocr.validation

import com.etix.features.ocr.domain.OCRSmartAnalyzer
import com.etix.features.ocr.engine.OCRProcessor
import com.etix.features.ocr.model.OCRResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test

/**
 * Tests OCR séparés, un par fichier et par champ (remplace à terme OCRValidationTest, laissé désactivé tel quel).
 *
 * Règle : seules les attentes ÉTABLIES par le texte du ticket sont actives (enseigne lisible, montant du TOTAL).
 * Les attentes qui dépendent d'une décision produit restent @Ignore avec la question bloquante
 * (docs/OCR_CAS_DE_REFERENCE.md) :
 *  - Q1 : « TOTAL » seul = libellé ou enseigne (ticket_003)
 *  - Q2 : taxonomie des catégories (Alimentation / Carburant / Transport…)
 *  - Q3 : lecture jj/mm d'une date ambiguë ; Q4 : la date fait-elle partie des attentes
 * Aucune attente n'est ajustée sur la sortie du moteur ; le moteur n'est pas modifié.
 */
class OCRFixturesTest {

    private fun run(file: String): OCRResult = OCRProcessor.process(OCRValidationUtils.loadText(file))
    private fun category(file: String) = OCRSmartAnalyzer.guessCategoryWithConfidence(OCRValidationUtils.loadText(file)).category

    // ---------------- ticket_001.txt : LIDL / TOTAL 34,50 ----------------
    @Test fun t001_enseigne() = assertEquals("LIDL", run("ticket_001.txt").merchant)
    @Test fun t001_montant() = assertEquals(34.50, run("ticket_001.txt").amount!!, 0.001)
    @Test fun t001_sans_date_dans_le_texte() = assertNull(run("ticket_001.txt").dateMillis)
    @Ignore("Bloqué Q2 (taxonomie) — proposition : Alimentation")
    @Test fun t001_categorie() = assertEquals("Alimentation", category("ticket_001.txt"))

    // ---------------- ticket_002.txt : CARREFOUR / TOTAL TTC 12,99 ----------------
    @Test fun t002_enseigne() = assertEquals("CARREFOUR", run("ticket_002.txt").merchant)
    @Test fun t002_montant() = assertEquals(12.99, run("ticket_002.txt").amount!!, 0.001)
    @Test fun t002_sans_date_dans_le_texte() = assertNull(run("ticket_002.txt").dateMillis)
    @Ignore("Bloqué Q2 (taxonomie) — proposition : Alimentation")
    @Test fun t002_categorie() = assertEquals("Alimentation", category("ticket_002.txt"))

    // ---------------- ticket_003.txt : TOTAL / TOTAL 58,20 ----------------
    @Test fun t003_montant() = assertEquals(58.20, run("ticket_003.txt").amount!!, 0.001)
    @Test fun t003_sans_date_dans_le_texte() = assertNull(run("ticket_003.txt").dateMillis)
    @Ignore("Bloqué Q1 — proposition : aucune enseigne (« TOTAL » = libellé). Le moteur renvoie aujourd'hui « TOTAL ».")
    @Test fun t003_enseigne() = assertNull(run("ticket_003.txt").merchant)
    @Ignore("Bloqué Q1/Q2 — proposition : aucune catégorie")
    @Test fun t003_categorie() = assertNull(category("ticket_003.txt"))

    // ---------------- ticket_004.txt : ESSO / 12/01/2026 / TOTAL TTC 23,45 € ----------------
    @Test fun t004_enseigne() = assertEquals("ESSO", run("ticket_004.txt").merchant)
    @Test fun t004_montant() = assertEquals(23.45, run("ticket_004.txt").amount!!, 0.001)
    @Ignore("Bloqué Q3/Q4 — proposition : 12 janvier 2026 (jj/mm)")
    @Test fun t004_date() {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = run("ticket_004.txt").dateMillis!! }
        assertEquals(12, c.get(java.util.Calendar.DAY_OF_MONTH)); assertEquals(java.util.Calendar.JANUARY, c.get(java.util.Calendar.MONTH))
    }
    @Ignore("Bloqué Q2 — proposition : Carburant (iOS). Le moteur Android ne connaît pas « esso » et range le carburant en Transport.")
    @Test fun t004_categorie() = assertEquals("Carburant", category("ticket_004.txt"))

    // ---------------- SYNTHÉTIQUES (rédigés à la main, pas de vrais tickets — ocr/synthetique/LISEZMOI.md) ----------------
    @Test fun s_restaurant_enseigne() = assertEquals("LE PETIT BISTROT", run("synthetique/restaurant_synthetique.txt").merchant)
    @Test fun s_restaurant_montant() = assertEquals(16.70, run("synthetique/restaurant_synthetique.txt").amount!!, 0.001)
    @Ignore("Bloqué Q2 — proposition : Restaurant")
    @Test fun s_restaurant_categorie() = assertEquals("Restaurant", category("synthetique/restaurant_synthetique.txt"))

    @Test fun s_long_enseigne() = assertEquals("CARREFOUR MARKET", run("synthetique/ticket_long_synthetique.txt").merchant)
    // Réactivé au lot 6 : défaut D1 corrigé (OCRAmountExtractor), attente inchangée (40,80 = TOTAL TTC).
    @Test fun s_long_montant_total_ttc() = assertEquals(40.80, run("synthetique/ticket_long_synthetique.txt").amount!!, 0.001)
    @Ignore("Bloqué Q4 — proposition : 15/09/2026 (non ambigu)")
    @Test fun s_long_date() {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = run("synthetique/ticket_long_synthetique.txt").dateMillis!! }
        assertEquals(15, c.get(java.util.Calendar.DAY_OF_MONTH)); assertEquals(java.util.Calendar.SEPTEMBER, c.get(java.util.Calendar.MONTH))
    }
    @Ignore("Bloqué Q2 — proposition : Alimentation")
    @Test fun s_long_categorie() = assertEquals("Alimentation", category("synthetique/ticket_long_synthetique.txt"))
}
