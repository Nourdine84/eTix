package com.etix.features.ocr.validation

import com.etix.features.ocr.engine.OCRAmountExtractor
import com.etix.features.ocr.engine.OCRProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Régression du choix du montant (lot 6, cas D1 iOS). Textes SYNTHÉTIQUES rédigés pour chaque règle ;
 * ils ne remplacent pas des tickets réels. Aucune dépendance aux catégories (Q2) ni à l'enseigne (Q1).
 */
class OCRAmountRegressionTest {

    private fun amount(vararg lines: String): Double? = OCRAmountExtractor.extract(lines.toList())
    private fun eq(expected: Double, actual: Double?) = assertEquals(expected, actual!!, 0.001)

    @Test fun sous_total_avant_total_ttc() = eq(40.80, amount("SOUS-TOTAL 42,80", "REMISE -2,00", "TOTAL TTC 40,80 €"))

    @Test fun variantes_de_sous_total_ignorees() {
        eq(19.90, amount("SOUS TOTAL 25,00", "BON DE REDUCTION -5,10", "TOTAL 19,90"))
        eq(19.90, amount("S/TOTAL 25,00", "TOTAL 19,90"))
        eq(19.90, amount("SUBTOTAL 25,00", "TOTAL 19,90"))
    }

    @Test fun remise_negative_jamais_retenue() = eq(9.00, amount("ARTICLE 10,00", "REMISE FIDELITE -1,00", "TOTAL 9,00"))

    @Test fun total_ht_et_tva_ignores_avant_ou_apres_le_ttc() {
        eq(12.00, amount("TOTAL HT 10,00", "TVA 20% 2,00", "TOTAL TTC 12,00"))
        eq(12.00, amount("TOTAL TTC 12,00", "TOTAL HT 10,00", "TVA 20% 2,00"))
        eq(12.00, amount("MONTANT HT 10,00", "TOTAL TVA 2,00", "TOTAL 12,00"))
    }

    @Test fun ttc_et_tva_sur_la_meme_ligne() = eq(40.80, amount("TOTAL TTC 40,80 DONT TVA 2,13"))

    @Test fun net_a_payer_prioritaire_sur_total() {
        eq(40.00, amount("TOTAL 45,00", "BON D ACHAT -5,00", "NET A PAYER 40,00"))
        eq(40.00, amount("TOTAL TTC 45,00", "AVOIR -5,00", "NET À PAYER 40,00 €"))
        eq(7.20, amount("TOTAL A PAYER 7,20", "TOTAL 7,20"))
    }

    @Test fun montant_paye_egal_au_total() = eq(40.80, amount("TOTAL TTC 40,80", "CB MASTERCARD 40,80", "RENDU 0,00"))

    @Test fun especes_superieures_au_total_on_garde_le_total() =
        eq(40.80, amount("TOTAL TTC 40,80", "ESPECES 50,00", "RENDU 9,20"))

    @Test fun sans_ligne_total_montant_paye_par_carte() = eq(12.30, amount("BOULANGERIE", "CARTE BANCAIRE 12,30"))

    @Test fun sans_ligne_total_especes_moins_rendu() = eq(12.50, amount("ESPECES 20,00", "RENDU MONNAIE 7,50"))

    @Test fun montant_sur_la_ligne_suivante_colonnes_separees() = eq(23.45, amount("TOTAL TTC", "23,45 €", "CB 23,45"))

    @Test fun milliers_et_symbole_euro() {
        eq(1234.56, amount("TOTAL TTC 1 234,56 EUR"))
        eq(1234.56, amount("TOTAL 1.234,56€"))
    }

    @Test fun plusieurs_totaux_le_dernier_est_retenu() =
        eq(18.00, amount("TOTAL 20,00", "ANNULATION ARTICLE", "TOTAL 18,00"))

    @Test fun libelle_total_sans_montant_puis_total_chiffre() = eq(58.20, amount("TOTAL", "TOTAL 58,20"))

    @Test fun sous_total_ttc_n_est_pas_le_total() = eq(40.80, amount("SOUS-TOTAL TTC 42,80", "REMISE -2,00", "TOTAL TTC 40,80"))

    @Test fun aucun_montant() = assertNull(amount("MERCI", "A BIENTOT"))

    @Test fun repli_sans_mot_cle_ignore_tva_et_rendu() = eq(8.50, amount("CAFE 2,50", "SANDWICH 6,00", "8,50", "TVA 10% 0,77"))

    /** Le ticket long synthétique complet, via le moteur : l'attente correcte (40,80) est conservée. */
    @Test fun ticket_long_synthetique_complet() =
        eq(40.80, OCRProcessor.process(OCRValidationUtils.loadText("synthetique/ticket_long_synthetique.txt")).amount)
}
