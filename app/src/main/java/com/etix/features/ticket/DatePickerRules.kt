package com.etix.features.ticket

/**
 * Présentation du sélecteur de date du formulaire ticket (Ajouter / Modifier), adaptée à l'espace disponible.
 *
 * Le calendrier (grille de 7 colonnes) n'est proposé par défaut que si un jour à deux chiffres y reste lisible à la
 * taille de police choisie par l'utilisateur. Sinon, le sélecteur s'ouvre directement en SAISIE (jj/mm/aaaa), format
 * indiqué dans le titre, en plein écran (fermeture et validation en haut, hors de portée du clavier, saisie défilante :
 * FullscreenPickerFit) ; la police n'est
 * jamais réduite. Le calendrier reste accessible par l'icône de bascule.
 *
 * Estimations calibrées sur l'émulateur (Material 1.12, 320 dp) : à police 2,0 la grille montrait « 1 » pour « 12 »
 * (colonne d'environ 31 dp, libellé d'environ 37 dp). Les tailles intermédiaires (police 1,3 à 1,8) ne sont pas
 * vérifiées par capture : la vérification émulateur contrôle la lisibilité réelle à police 1,0 et 2,0.
 */
object DatePickerRules {

    const val TITLE = "Date du ticket"
    const val TITLE_TEXT_INPUT = "Date du ticket (jj/mm/aaaa)"

    /** Largeur estimée d'une colonne de jours (dp) : marges du dialogue et du calendrier retirées, 7 colonnes. */
    fun calendarColumnDp(screenWidthDp: Int): Float = (minOf(screenWidthDp, 480) - 100) / 7f

    /** Largeur estimée nécessaire pour un jour à deux chiffres (dp), avec une marge de sécurité. */
    fun twoDigitDayDp(fontScale: Float): Float = 16.75f * fontScale + 4f

    /** Vrai : ouvrir le sélecteur en saisie (le calendrier ne serait pas lisible). */
    fun prefersTextInput(screenWidthDp: Int, fontScale: Float): Boolean =
        twoDigitDayDp(fontScale) > calendarColumnDp(screenWidthDp)
}
