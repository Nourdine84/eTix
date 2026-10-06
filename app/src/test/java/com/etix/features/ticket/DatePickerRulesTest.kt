package com.etix.features.ticket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatePickerRulesTest {

    // Libellés de mois mesurés sur l'émulateur (API 34) : « Octobre 2026 » ≈ 136 dp à police 1,5 (coupé)
    private val moisCourt = 100f
    private val moisCoupe = 136f

    @Test
    fun calendrier_par_defaut_a_police_normale() {
        assertFalse(DatePickerRules.prefersTextInput(320, 1.0f, moisCourt))
        assertFalse(DatePickerRules.prefersTextInput(360, 1.0f, moisCourt))
        assertFalse(DatePickerRules.prefersTextInput(411, 1.3f, moisCourt))
    }

    @Test
    fun saisie_par_defaut_a_320dp_police_2() {
        // Cas constaté sur émulateur : « 1 » affiché pour « 12 » dans la grille
        assertTrue(DatePickerRules.prefersTextInput(320, 2.0f, moisCourt))
        assertTrue(DatePickerRules.prefersTextInput(360, 2.0f, moisCourt))
    }

    @Test
    fun ecran_plus_large_sans_colonne_plus_large() {
        // Material 1.12 : calendrier en fenêtre de largeur fixe (288 dp) ; un écran de 411 dp n'élargit pas les colonnes
        assertEquals(DatePickerRules.calendarColumnDp(320), DatePickerRules.calendarColumnDp(411), 0.001f)
        assertTrue(DatePickerRules.prefersTextInput(411, 2.0f, moisCourt))
    }

    @Test
    fun saisie_quand_le_mois_ne_tient_pas() {
        // Constaté sur émulateur à 320 dp police 1,5 : jours lisibles, « Octobre 2026 » coupé
        assertFalse(DatePickerRules.twoDigitDayDp(1.5f) > DatePickerRules.calendarColumnDp(320))
        assertTrue(DatePickerRules.prefersTextInput(320, 1.5f, moisCoupe))
        assertTrue(DatePickerRules.prefersTextInput(411, 1.0f, DatePickerRules.MONTH_LABEL_AVAILABLE_DP + 1f))
        assertFalse(DatePickerRules.prefersTextInput(320, 1.0f, DatePickerRules.MONTH_LABEL_AVAILABLE_DP))
    }

    @Test
    fun le_titre_de_la_saisie_indique_le_format() {
        assertTrue(DatePickerRules.TITLE_TEXT_INPUT.contains("jj/mm/aaaa"))
    }
}
