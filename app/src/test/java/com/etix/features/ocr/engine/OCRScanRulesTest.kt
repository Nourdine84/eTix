package com.etix.features.ocr.engine

import com.etix.features.ticket.TicketFormRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Lot 9 — règles OCR validées (Q1 à Q4) : cas limites au-delà des fichiers de référence.
 * Aucune attente n'est calquée sur la sortie du moteur : chaque cas découle d'une décision écrite.
 */
class OCRScanRulesTest {

    private fun merchant(vararg lines: String) = OCRProcessor.process(lines.joinToString("\n")).merchant
    private fun category(vararg lines: String) = OCRCategoryGuesser.guess(lines.joinToString("\n"))?.category

    // ---------- Q1 : « TOTAL » seul = libellé ; TotalEnergies = enseigne ----------
    @Test fun total_seul_n_est_pas_une_enseigne() {
        assertNull(merchant("TOTAL", "TOTAL 58,20"))
        assertNull(merchant("TOTAL TTC", "23,45"))
        assertNull(merchant("NET A PAYER", "12,00"))
        assertNull(merchant("Total", "Total 10,00"))
    }

    @Test fun libelles_et_politesse_ignores_au_profit_de_l_enseigne() {
        assertEquals("BOULANGERIE MARTIN", merchant("TOTAL", "BOULANGERIE MARTIN", "TOTAL 4,20"))
        assertNull(merchant("TOTAL", "TOTAL 58,20", "MERCI DE VOTRE VISITE"))
        assertNull(merchant("CB VISA", "A BIENTOT"))
    }

    @Test fun totalenergies_reste_une_enseigne_carburant() {
        assertEquals("TOTALENERGIES", merchant("TOTALENERGIES", "RELAIS DE LA FORET", "TOTAL TTC 61,30"))
        assertEquals("Carburant", category("TOTALENERGIES", "RELAIS DE LA FORET", "TOTAL TTC 61,30"))
        assertEquals("TOTAL ENERGIES", merchant("TOTAL ENERGIES", "TOTAL 45,00"))
        assertEquals("Carburant", category("TOTAL ENERGIES", "TOTAL 45,00"))
        assertEquals("TotalEnergies", merchant("TotalEnergies", "Total 45,00"))
        assertEquals("Carburant", category("TotalEnergies", "Total 45,00"))
    }

    // ---------- Q2 : catégories de référence iOS ----------
    @Test fun dictionnaire_ios_sur_l_enseigne() {
        assertEquals("Carburant", category("ESSO", "TOTAL TTC 23,45"))
        assertEquals("Carburant", category("STATION BP", "TOTAL 30,00"))
        assertEquals("Alimentation", category("LIDL", "TOTAL 34,50"))
        assertEquals("Alimentation", category("SUPER U", "TOTAL 9,10"))
        assertEquals("Vêtements", category("H&M", "TOTAL 19,99"))
        assertEquals("Maison", category("LEROY MERLIN", "TOTAL 89,00"))
        assertEquals("Restaurant", category("MCDONALD'S", "TOTAL 11,40"))
        assertEquals("Restaurant", category("KFC", "TOTAL 11,40"))
        assertEquals("KFC", merchant("KFC", "TOTAL 11,40"))
    }

    @Test fun correspondance_par_mots_entiers() {
        // iOS « contient » : « but » et « bp » pouvaient correspondre à l'intérieur d'un mot
        assertNull(OCRCategoryGuesser.fromStore("BUTCHER SHOP"))
        assertNull(OCRCategoryGuesser.fromStore("ABPRO SERVICES"))
        assertEquals("Maison", OCRCategoryGuesser.fromStore("BUT"))
    }

    @Test fun articles_ne_changent_pas_la_categorie_de_l_enseigne() {
        assertEquals("Alimentation", category("CARREFOUR MARKET", "15/09/2026", "CAFE MOULU 3,85", "TOTAL TTC 3,85"))
        // enseigne inconnue, « CAFE » seulement dans les articles (au-delà de l'en-tête) : aucune catégorie
        assertNull(category("MAISON DUPONT", "12 RUE HAUTE", "75001 PARIS", "TEL 0102030405", "CAFE 2,20", "TOTAL 2,20"))
    }

    @Test fun mot_d_activite_dans_l_en_tete() {
        assertEquals("Restaurant", category("LE PETIT BISTROT", "12 RUE DES LILAS", "TABLE 4 COUVERTS 2", "TOTAL 16,70"))
        assertEquals("Santé", category("PHARMACIE DU CENTRE", "TOTAL 8,90"))
    }

    @Test fun sans_enseigne_aucune_categorie() {
        assertNull(category("TOTAL", "TOTAL 58,20"))
        assertNull(category(""))
    }

    @Test fun toutes_les_categories_proposees_sont_des_categories_ios() {
        val ios = TicketFormRules.SYSTEM_CATEGORIES
        assertTrue(OCRCategoryGuesser.KNOWN_STORES.values.all { it in ios })
        for (header in listOf("RESTAURANT", "STATION SERVICE", "PHARMACIE", "BOULANGERIE", "CINEMA", "PARKING",
                "COIFFURE", "BRICOLAGE")) {
            val c = category("ENSEIGNE INCONNUE $header", "TOTAL 1,00")
            assertTrue("$header → $c", c in ios)
        }
    }

    // ---------- Q3 / Q4 : dates jour/mois/année vérifiées ----------
    private fun dayMonthYear(vararg lines: String): Triple<Int, Int, Int>? =
        OCRProcessor.process(lines.joinToString("\n")).dateMillis?.let {
            val c = Calendar.getInstance().apply { timeInMillis = it }
            Triple(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
        }

    @Test fun dates_jour_mois_annee() {
        assertEquals(Triple(12, 1, 2026), dayMonthYear("ESSO", "12/01/2026", "TOTAL 23,45"))
        assertEquals(Triple(1, 12, 2026), dayMonthYear("ESSO", "01/12/2026", "TOTAL 23,45"))
        assertEquals(Triple(3, 10, 2026), dayMonthYear("LIDL", "03.10.26 18:42", "TOTAL 5,00"))
        assertEquals(Triple(3, 10, 2026), dayMonthYear("LIDL", "TEL 01.23.45.67.89 LE 03/10/2026", "TOTAL 5,00"))
        assertNull(dayMonthYear("LIDL", "13/13/2026", "TOTAL 5,00"))
    }
}
