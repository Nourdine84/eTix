package com.etix.features.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.etix.features.store.TimeRange

/**
 * Lot 10 — choix du thème (iOS AppAppearance : Système / Clair / Sombre).
 * Stocké tel qu'avant dans les préférences de session (`theme_mode`, valeurs AppCompatDelegate) : un choix déjà
 * enregistré par l'ancien bouton « Changer de thème » (clair ou sombre) est conservé tel quel.
 */
enum class ThemeChoice(val mode: Int, val label: String) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, "Système"),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, "Clair"),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, "Sombre");

    companion object {
        /** Mode enregistré → choix affiché. Toute autre valeur (aucune préférence, mode inconnu) = Système. */
        fun fromMode(mode: Int): ThemeChoice = when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> LIGHT
            AppCompatDelegate.MODE_NIGHT_YES -> DARK
            else -> SYSTEM
        }
    }
}

/**
 * Lot 10 — préférences d'affichage (iOS AppSettings.defaultRange). SharedPreferences locales `etix_settings` ;
 * aucune donnée Room, aucun réseau. Absence de préférence = « Ce mois » (TimeRange.DEFAULT, comme iOS).
 */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var defaultRange: TimeRange
        get() = prefs.getString(KEY_DEFAULT_RANGE, null)
            ?.let { runCatching { TimeRange.valueOf(it) }.getOrNull() } ?: TimeRange.DEFAULT
        set(value) { prefs.edit().putString(KEY_DEFAULT_RANGE, value.name).apply() }

    companion object {
        const val PREFS = "etix_settings"
        const val KEY_DEFAULT_RANGE = "default_range"

        /**
         * Période d'un écran à sa création : écran neuf → période par défaut ; écran recréé (rotation, thème,
         * processus relancé) → période choisie sur l'écran, conservée même si le réglage a changé entre-temps.
         */
        fun initialRange(savedRange: TimeRange?, currentDefault: TimeRange): TimeRange = savedRange ?: currentDefault
    }
}
