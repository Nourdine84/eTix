package com.etix.features.ocr.engine

import java.text.Normalizer
import java.util.Locale

/**
 * Catégorie suggérée pour un ticket scanné (lot 9, décision Q2 : catégories de référence iOS).
 *
 * 1. Enseigne reconnue dans le dictionnaire iOS `StoreCategoryMapper.knownStores` (mêmes clés, mêmes catégories),
 *    comparée par mots entiers sur le nom d'enseigne extrait (ESSO → Carburant, LIDL → Alimentation).
 *    Écarts Android assumés : correspondance par mots entiers (iOS : « contient », « bp » ou « but » pouvaient
 *    correspondre à l'intérieur d'un autre mot) et ordre déterministe (clé la plus longue d'abord) ;
 *    les clés longues sont aussi cherchées sans espaces (« TOTAL ENERGIES » → totalenergies).
 * 2. Sinon, mots-clés d'activité dans l'en-tête du ticket (4 premières lignes : enseigne, adresse, table…),
 *    rangés dans les catégories iOS. Les lignes d'articles ne sont pas lues (« CAFE MOULU » dans un ticket
 *    de supermarché ne doit pas en faire un restaurant).
 * 3. Sans enseigne détectée (« TOTAL » seul, décision Q1) : aucune catégorie.
 *
 * L'historique iOS (catégorie la plus fréquente pour ce magasin) n'est pas porté ici : il dépend de la base
 * et sera appliqué par l'écran, jamais en modifiant les tickets existants.
 */
object OCRCategoryGuesser {

    enum class Source { STORE_DICTIONARY, HEADER_KEYWORDS }

    data class Guess(val category: String, val source: Source)

    /** iOS StoreCategoryMapper.knownStores, recopié tel quel (catégories système iOS). */
    val KNOWN_STORES: Map<String, String> = linkedMapOf(
        // Grande distribution
        "carrefour" to "Alimentation", "leclerc" to "Alimentation", "lidl" to "Alimentation",
        "aldi" to "Alimentation", "intermarche" to "Alimentation", "super u" to "Alimentation",
        "monoprix" to "Alimentation", "franprix" to "Alimentation", "picard" to "Alimentation",
        "biocoop" to "Alimentation", "naturalia" to "Alimentation", "auchan" to "Alimentation",
        "cora" to "Alimentation", "netto" to "Alimentation",
        // Restauration rapide
        "mcdonald" to "Restaurant", "mcdo" to "Restaurant", "burger king" to "Restaurant", "kfc" to "Restaurant",
        "subway" to "Restaurant", "domino" to "Restaurant", "pizza hut" to "Restaurant",
        "starbucks" to "Restaurant", "five guys" to "Restaurant", "paul" to "Alimentation",
        // Sport
        "decathlon" to "Sport", "go sport" to "Sport", "sport 2000" to "Sport",
        // Santé
        "pharmacie" to "Santé", "pharma" to "Santé", "optical" to "Santé", "opticien" to "Santé",
        // Maison / Bricolage
        "leroy merlin" to "Maison", "bricorama" to "Maison", "mr bricolage" to "Maison", "ikea" to "Maison",
        "but" to "Maison", "conforama" to "Maison",
        // Vêtements
        "zara" to "Vêtements", "h&m" to "Vêtements", "uniqlo" to "Vêtements", "kiabi" to "Vêtements",
        "celio" to "Vêtements", "jules" to "Vêtements",
        // Carburant
        "totalenergies" to "Carburant", "bp" to "Carburant", "shell" to "Carburant", "esso" to "Carburant",
        // Transport
        "sncf" to "Transport", "ratp" to "Transport", "ouigo" to "Transport", "uber" to "Transport",
        "blablacar" to "Transport",
        // Culture / High-Tech
        "fnac" to "Culture", "cultura" to "Culture", "darty" to "High-Tech", "boulanger" to "High-Tech",
        // Beauté
        "sephora" to "Beauté", "nocibe" to "Beauté", "yves rocher" to "Beauté",
        // Abonnements
        "netflix" to "Abonnement", "spotify" to "Abonnement", "amazon" to "Abonnement", "apple" to "Abonnement",
    )

    /** Mots d'activité lus dans l'en-tête (catégories iOS). */
    private val HEADER_KEYWORDS: List<Pair<String, String>> = listOf(
        "restaurant" to "Restaurant", "brasserie" to "Restaurant", "bistrot" to "Restaurant",
        "bistro" to "Restaurant", "pizzeria" to "Restaurant", "creperie" to "Restaurant",
        "couverts" to "Restaurant", "traiteur" to "Restaurant",
        "station service" to "Carburant", "carburant" to "Carburant", "carburants" to "Carburant",
        "pharmacie" to "Santé", "laboratoire" to "Santé", "cabinet medical" to "Santé",
        "boulangerie" to "Alimentation", "boucherie" to "Alimentation", "epicerie" to "Alimentation",
        "supermarche" to "Alimentation", "hypermarche" to "Alimentation", "primeur" to "Alimentation",
        "fromagerie" to "Alimentation",
        "cinema" to "Culture", "theatre" to "Culture", "librairie" to "Culture", "musee" to "Culture",
        "peage" to "Transport", "parking" to "Transport", "taxi" to "Transport",
        "coiffure" to "Beauté", "parfumerie" to "Beauté",
        "bricolage" to "Maison", "jardinerie" to "Maison", "quincaillerie" to "Maison",
    )

    fun guess(rawText: String): Guess? {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val merchant = OCRProcessor.extractMerchant(lines) ?: return null
        fromStore(merchant)?.let { return Guess(it, Source.STORE_DICTIONARY) }
        fromHeader(lines.take(4))?.let { return Guess(it, Source.HEADER_KEYWORDS) }
        return null
    }

    /** Catégorie iOS d'une enseigne connue, ou null. */
    fun fromStore(storeName: String): String? {
        val n = normalize(storeName)
        if (n.isBlank()) return null
        val padded = " $n "
        val compact = n.replace(" ", "")
        for ((key, category) in KNOWN_STORES.entries.sortedByDescending { it.key.length }) {
            if (padded.contains(" $key ")) return category
            if (key.length >= 6 && !key.contains(' ') && compact.contains(key)) return category
        }
        return null
    }

    private fun fromHeader(lines: List<String>): String? {
        val padded = " " + normalize(lines.joinToString(" ")) + " "
        return HEADER_KEYWORDS.firstOrNull { padded.contains(" ${it.first} ") }?.second
    }

    internal fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9&]+"), " ").trim()
}
