package com.etix.features.ticket

import java.util.Calendar
import java.util.TimeZone

/** Règles du formulaire ticket (Ajout / Édition) — portage iOS AmountParser + CategoryPickerSheet. */
object TicketFormRules {

    /** iOS AmountParser : espaces retirés, virgule → point, strictement positif. */
    fun parseAmount(raw: String): Double? =
        raw.trim().replace(" ", "").replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && !it.isInfinite() }

    /** Montant affiché dans le champ d'édition : « 12,50 » (iOS « %.2f », virgule FR). */
    fun formatAmountForInput(v: Double): String = String.format(java.util.Locale.FRANCE, "%.2f", v)

    /** Catégories système iOS (CategoryPickerSheet.systemCategories), même ordre. */
    val SYSTEM_CATEGORIES = listOf(
        "Alimentation", "Restaurant", "Transport", "Santé", "Sport",
        "Maison", "Vêtements", "Culture", "High-Tech", "Beauté",
        "Carburant", "Abonnement"
    )

    /**
     * Valeur enregistrée quand aucune catégorie n'est choisie.
     * iOS enregistre une chaîne vide ; Android conserve « Autre » (comportement existant,
     * attendu par l'écran Catégories V1). Écart documenté.
     */
    const val DEFAULT_CATEGORY = "Autre"

    /** iOS : système + catégories déjà utilisées (hors système, hors « Autre », non vides), triées. */
    fun pickerCategories(used: Collection<String>): List<String> {
        val extra = used.map { it.trim() }
            .filter { it.isNotEmpty() && it !in SYSTEM_CATEGORIES && it != DEFAULT_CATEGORY }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        return SYSTEM_CATEGORIES + extra
    }

    /**
     * Combine le jour choisi dans MaterialDatePicker (minuit UTC) avec l'heure d'un instant de référence
     * (heure actuelle pour un ajout, heure d'origine pour une édition), en heure locale.
     */
    fun combineDay(pickerUtcMillis: Long, keepTimeFrom: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerUtcMillis }
        return Calendar.getInstance().apply {
            timeInMillis = keepTimeFrom
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** Jour local d'un instant, exprimé en minuit UTC (sélection initiale de MaterialDatePicker). */
    fun toPickerSelection(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
}
