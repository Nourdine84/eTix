package com.etix.features.ocr.validation

import com.etix.features.ocr.engine.OCRAmountExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Montants AMBIGUS — tests de CARACTÉRISATION (lot 7) : ils figent le comportement ACTUEL de la règle de sélection,
 * y compris quand il est discutable ou faux. Ils ne valident pas ce comportement : ils garantissent qu'aucun
 * changement de la règle ne passera inaperçu (le test échouera et devra être mis à jour explicitement).
 * Chaque cas indique la lecture correcte probable, qui reste à décider (docs/OCR_CAS_DE_REFERENCE.md, « Limites »).
 * Textes synthétiques.
 */
class OCRAmountAmbiguityTest {

    private fun amount(vararg lines: String) = OCRAmountExtractor.extract(lines.toList())

    /** L1 — « 2 125,00 » : 2 125,00 € (milliers) ou quantité 2 × 125,00 ? Lu comme 2 125,00. */
    @Test fun L1_espace_entre_quantite_et_prix_lu_comme_milliers() {
        assertEquals(2125.00, amount("TOTAL 2 125,00")!!, 0.001)
        assertEquals(2125.00, amount("2 125,00")!!, 0.001)
    }

    /** L2 — sans mot-clé : le plus grand montant (6,00), pas la somme (8,50). */
    @Test fun L2_sans_mot_cle_plus_grand_montant_pas_la_somme() =
        assertEquals(6.00, amount("CAFE 2,50", "SANDWICH 6,00")!!, 0.001)

    /** L3 — format anglo-saxon « 1,234.56 » : lu 234,56 (DÉFAUT probable ; ticket étranger). */
    @Test fun L3_format_anglo_saxon_mal_lu() =
        assertEquals(234.56, amount("TOTAL 1,234.56")!!, 0.001)

    /** L4 — total négatif (remboursement) : aucun montant (un ticket ne peut pas être négatif dans l'app). */
    @Test fun L4_total_negatif_rembourse_aucun_montant() = assertNull(amount("TOTAL -5,00", "AVOIR 5,00"))

    /** L5 — espèces sans ligne de rendu ni total : le montant remis (50,00), pas forcément le montant dû. */
    @Test fun L5_especes_sans_rendu_ni_total() = assertEquals(50.00, amount("ESPECES 50,00")!!, 0.001)

    /** L6 — deux lignes « TOTAL TTC » différentes (ticket dupliqué / corrigé) : la dernière (12,00). */
    @Test fun L6_deux_totaux_ttc_la_derniere() =
        assertEquals(12.00, amount("TOTAL TTC 10,00", "TOTAL TTC 12,00")!!, 0.001)

    /** L7 — trois décimales « 12,500 » : non reconnu ; repli sur le paiement carte (12,50). */
    @Test fun L7_trois_decimales_non_reconnu_repli_paiement() =
        assertEquals(12.50, amount("TOTAL 12,500", "CB 12,50")!!, 0.001)

    /** L8 — milliers avec point « 12.345,67 » : lu 12 345,67. */
    @Test fun L8_milliers_avec_point() = assertEquals(12345.67, amount("TOTAL 12.345,67")!!, 0.001)
}
