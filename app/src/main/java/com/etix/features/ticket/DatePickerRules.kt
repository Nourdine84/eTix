package com.etix.features.ticket

/**
 * Présentation du sélecteur de date du formulaire ticket (Ajouter / Modifier), adaptée à l'espace disponible.
 *
 * Le calendrier n'est proposé par défaut que si, à la taille de police choisie par l'utilisateur, un jour à deux
 * chiffres tient dans une colonne ET le libellé du mois le plus long (« Septembre 2026 ») tient dans l'en-tête du
 * calendrier. Sinon, le sélecteur s'ouvre directement en SAISIE (jj/mm/aaaa), format indiqué dans le titre, en plein
 * écran (fermeture et validation en haut, hors de portée du clavier, saisie défilante : FullscreenPickerFit) ; la
 * police n'est jamais réduite. Le calendrier reste accessible par l'icône de bascule.
 *
 * Material 1.12 donne au calendrier en fenêtre une largeur FIXE de 288 dp (2 × 12 + 7 × 36 + 6 × 2), quelle que soit la
 * largeur de l'écran : un écran plus large n'offre ni colonne ni en-tête plus large. Estimations calibrées sur
 * l'émulateur (API 34, 320 dp) : à police 2,0 la grille montrait « 1 » pour « 12 » (colonne d'environ 31 dp, libellé
 * d'environ 37 dp) ; à police 1,5, « Octobre 2026 » demandait 136 dp pour 114 dp disponibles (jours lisibles).
 */
object DatePickerRules {

    const val TITLE = "Date du ticket"
    const val TITLE_TEXT_INPUT = "Date du ticket (jj/mm/aaaa)"

    /** Largeur disponible pour le libellé du mois dans l'en-tête du calendrier (dp), mesurée : 258 px à 2,25. */
    const val MONTH_LABEL_AVAILABLE_DP = 114f

    /**
     * Largeur estimée d'une colonne de jours (dp) : marges du dialogue et du calendrier retirées, 7 colonnes. Le
     * calendrier ne s'élargit pas au-delà de 320 dp d'écran (largeur fixe).
     */
    fun calendarColumnDp(screenWidthDp: Int): Float = (minOf(screenWidthDp, 320) - 100) / 7f

    /** Largeur estimée nécessaire pour un jour à deux chiffres (dp), avec une marge de sécurité. */
    fun twoDigitDayDp(fontScale: Float): Float = 16.75f * fontScale + 4f

    /**
     * Vrai : ouvrir le sélecteur en saisie (le calendrier ne serait pas lisible). [longestMonthLabelDp] : largeur du
     * libellé de mois le plus long de l'année, mesurée sur l'appareil avec la police de l'en-tête du calendrier.
     */
    fun prefersTextInput(screenWidthDp: Int, fontScale: Float, longestMonthLabelDp: Float): Boolean =
        twoDigitDayDp(fontScale) > calendarColumnDp(screenWidthDp) || longestMonthLabelDp > MONTH_LABEL_AVAILABLE_DP
}
