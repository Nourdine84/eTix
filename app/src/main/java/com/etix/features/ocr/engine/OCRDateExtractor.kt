package com.etix.features.ocr.engine

import java.util.TimeZone

/**
 * Extraction date v2:
 * - Supporte dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy
 * - Supporte aussi dd/MM/yy etc.
 * Retourne dateMillis (startOfDay).
 *
 * Compatibilité (lot 8) : n'utilise plus java.time (API 26, absent d'Android 5 à 7.1 :
 * NoClassDefFoundError constaté sur émulateur API 21). Les règles sont celles de
 * `LocalDate.parse(texte, DateTimeFormatter.ofPattern(motif))` en mode par défaut, reproduites
 * à l'identique et vérifiées contre java.time sur la JVM (OCRDateExtractorEquivalenceTest) :
 * - jour et mois : exactement 2 chiffres ASCII ; séparateur identique au motif ;
 * - « yyyy » : au moins 4 chiffres ; plus de 4 seulement précédés de « + » ; « - » refusé ;
 * - « yy » : exactement 2 chiffres, année 2000 à 2099 ;
 * - le texte entier doit correspondre ; jour 01–31, mois 01–12, année 1 à 999 999 999 ;
 * - jour au-delà de la fin du mois ramené au dernier jour (31/04 → 30/04, 30/02 → 28 ou 29/02) ;
 * - début du jour dans le fuseau de l'appareil ; si minuit n'existe pas (changement d'heure),
 *   premier instant du jour ; si minuit existe deux fois, le premier.
 * Voir docs/COMPATIBILITE_ANDROID.md.
 */
object OCRDateExtractor {

    private val patterns = listOf(
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "dd.MM.yyyy",
        "dd/MM/yy",
        "dd-MM-yy",
        "dd.MM.yy"
    )

    /** Date civile (calendrier grégorien proleptique, comme java.time). */
    private class Ymd(val year: Int, val month: Int, val day: Int)

    /**
     * Lot 9 (décisions Q3 / Q4, dates lues jour/mois/année et vérifiées) : avant les règles d'origine, une date
     * délimitée dans la ligne telle qu'elle est lue (espaces conservés) est prise en priorité. Corrige
     * « 03.10.26 18:42 » : une fois les espaces retirés (« 03.10.2618:42 »), les règles d'origine lisaient
     * l'année 2618. Hors de ce cas, comportement inchangé ([extractDateMillisOriginalRules]).
     */
    fun extractDateMillis(lines: List<String>): Long? {
        for (line in lines.map { it.trim() }.filter { it.isNotBlank() }) {
            val parsed = delimitedDate(line) ?: tryParse(line.replace(" ", "")) ?: continue
            return startOfDayMillis(parsed) ?: continue
        }
        return null
    }

    /**
     * Lot 9 — nombre de dates différentes lisibles (jj?mm?aaaa ou jj?mm?aa délimitées) dans le texte. Sert
     * uniquement à signaler une date ambiguë (ex. date d'achat et date de validité) : la date retenue par
     * [extractDateMillis] est inchangée.
     */
    fun distinctDateCount(lines: List<String>): Int = lines.flatMap { line ->
        DELIMITED.findAll(line).mapNotNull { m ->
            val (d, mo, y) = m.destructured
            parse("$d/$mo/$y", if (y.length == 4) "dd/MM/yyyy" else "dd/MM/yy")?.let { "${it.year}-${it.month}-${it.day}" }
        }.toList()
    }.distinct().size

    /** Date jj?mm?aaaa ou jj?mm?aa non collée à d'autres chiffres, séparateurs « / », « - » ou « . ». */
    private val DELIMITED = Regex("""(?<!\d)(\d{2})[./-](\d{2})[./-](\d{4}|\d{2})(?!\d)""")

    private fun delimitedDate(line: String): Ymd? = DELIMITED.findAll(line).firstNotNullOfOrNull { m ->
        val (d, mo, y) = m.destructured
        parse("$d/$mo/$y", if (y.length == 4) "dd/MM/yyyy" else "dd/MM/yy")
    }

    /** Règles d'origine (portage exact de java.time, lot 8), sans la priorité aux dates délimitées. */
    internal fun extractDateMillisOriginalRules(lines: List<String>): Long? {
        val cleaned = lines
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (line in cleaned) {
            val candidate = line.replace(" ", "")
            val parsed = tryParse(candidate) ?: continue
            // Années au-delà de ~292 millions : hors de la plage des millisecondes (java.time levait
            // ArithmeticException). Ligne ignorée plutôt que plantage.
            return startOfDayMillis(parsed) ?: continue
        }
        return null
    }

    private fun tryParse(text: String): Ymd? {
        for (p in patterns) {
            parse(text, p)?.let { return it }
        }

        // fallback : détecter une date au milieu d'une ligne (ex "DATE: 12/01/2026")
        val regex = Regex("""(\d{2}[./-]\d{2}[./-]\d{2,4})""")
        val m = regex.find(text)?.groupValues?.getOrNull(1) ?: return null
        val normalized = m.replace('-', '/').replace('.', '/')

        // essayer avec 2 formats
        val tryFormats = listOf("dd/MM/yyyy", "dd/MM/yy")
        for (p in tryFormats) {
            parse(normalized, p)?.let { return it }
        }
        return null
    }

    /** [pattern] : « dd?MM?yyyy » ou « dd?MM?yy », « ? » étant le séparateur. */
    private fun parse(text: String, pattern: String): Ymd? {
        val sep = pattern[2]
        val fourDigitYear = pattern.length == 10
        if (text.length < 8) return null
        val day = twoDigits(text, 0) ?: return null
        if (text[2] != sep) return null
        val month = twoDigits(text, 3) ?: return null
        if (text[5] != sep) return null
        val rest = text.substring(6)
        val year: Long = if (fourDigitYear) {
            val positive = rest.startsWith("+")
            if (rest.startsWith("-")) return null          // année d'ère négative ou nulle : refusée
            val digits = if (positive) rest.substring(1) else rest
            if (digits.length < 4 || digits.length > 19 || !digits.all { it in '0'..'9' }) return null
            if (!positive && digits.length > 4) return null // plus de 4 chiffres : « + » obligatoire
            if (positive && digits.length <= 4) return null // « + » seulement au-delà de 4 chiffres
            val v = digits.toBigInteger()
            if (v < java.math.BigInteger.ONE || v > MAX_YEAR.toBigInteger()) return null
            v.toLong()
        } else {
            if (rest.length != 2) return null
            2000L + (twoDigits(rest, 0) ?: return null)
        }
        if (month !in 1..12 || day !in 1..31) return null
        val y = year.toInt()
        val d = when (month) {
            4, 6, 9, 11 -> minOf(day, 30)
            2 -> minOf(day, if (isLeap(y)) 29 else 28)
            else -> day
        }
        return Ymd(y, month, d)
    }

    private fun twoDigits(s: String, at: Int): Int? {
        if (at + 2 > s.length) return null
        val a = s[at]; val b = s[at + 1]
        if (a !in '0'..'9' || b !in '0'..'9') return null
        return (a - '0') * 10 + (b - '0')
    }

    private fun isLeap(y: Int) = (y % 4 == 0) && (y % 100 != 0 || y % 400 == 0)

    /** Jours depuis le 1970-01-01, calendrier grégorien proleptique (algorithme de LocalDate.toEpochDay). */
    private fun epochDay(d: Ymd): Long {
        val y = d.year.toLong(); val m = d.month.toLong()
        var total = 365 * y
        total += if (y >= 0) (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400 else -(y / -4 - y / -100 + y / -400)
        total += (367 * m - 362) / 12
        total += d.day - 1
        if (m > 2) { total--; if (!isLeap(d.year)) total-- }
        return total - DAYS_0000_TO_1970
    }

    private fun startOfDayMillis(d: Ymd): Long? {
        val day = epochDay(d)
        if (day > Long.MAX_VALUE / DAY_MS - 1 || day < Long.MIN_VALUE / DAY_MS + 1) return null
        val local = day * DAY_MS
        val tz = TimeZone.getDefault()
        val before = tz.getOffset(local - DAY_MS)
        val after = tz.getOffset(local + DAY_MS)
        return when {
            tz.getOffset(local - before) == before -> local - before // heure valide (ou 1re d'une heure doublée)
            tz.getOffset(local - after) == after -> local - after
            else -> local - before                                    // minuit sauté : 1er instant du jour
        }
    }

    private const val DAY_MS = 86_400_000L
    private const val MAX_YEAR = 999_999_999L
    private const val DAYS_0000_TO_1970 = 146097L * 5L - (30L * 365L + 7L)
}
