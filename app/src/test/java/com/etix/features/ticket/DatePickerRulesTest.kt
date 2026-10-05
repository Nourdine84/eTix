package com.etix.features.ticket

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatePickerRulesTest {

    @Test
    fun calendrier_par_defaut_a_police_normale() {
        assertFalse(DatePickerRules.prefersTextInput(320, 1.0f))
        assertFalse(DatePickerRules.prefersTextInput(360, 1.0f))
        assertFalse(DatePickerRules.prefersTextInput(411, 1.3f))
    }

    @Test
    fun saisie_par_defaut_a_320dp_police_2() {
        // Cas constaté sur émulateur : « 1 » affiché pour « 12 » dans la grille
        assertTrue(DatePickerRules.prefersTextInput(320, 2.0f))
        assertTrue(DatePickerRules.prefersTextInput(360, 2.0f))
    }

    @Test
    fun le_titre_de_la_saisie_indique_le_format() {
        assertTrue(DatePickerRules.TITLE_TEXT_INPUT.contains("jj/mm/aaaa"))
    }
}
