package com.etix.features.ocr.engine

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Random
import java.util.TimeZone

/**
 * Lot 9 : porte sur extractDateMillisOriginalRules (règles d'origine) ; extractDateMillis y ajoute la priorité aux
 * dates délimitées (décisions Q3 / Q4), couverte par OCRDateExtractorTest et OCRScanRulesTest.
 *
 * Équivalence stricte entre le lecteur de dates OCR sans java.time (compatible API 21) et
 * l'implémentation d'origine en java.time (OCRDateExtractorJavaTimeReference), sur la JVM :
 * - lecture (jour, mois, année, rejets) : entrées construites (formats, séparateurs, bornes, signes,
 *   préfixes) et 40 000 entrées aléatoires, en UTC, toutes années ;
 * - début du jour : chaque date du 01/01/1901 au 31/12/2086 dans 11 fuseaux, dont plusieurs changent
 *   d'heure à minuit (Sao Paulo, Santiago, La Havane, Beyrouth, Asuncion) ou ont sauté un jour (Apia).
 *
 * Différences admises, hors de ces plages :
 * - années au-delà de ~292 millions (« +300000000 ») : java.time levait ArithmeticException
 *   (plantage) ; la nouvelle version ignore la ligne ;
 * - avant 1901 et après 2086, le début du jour peut différer de l'heure locale historique (LMT) ou
 *   des règles futures calculées : la table des fuseaux de java.util.TimeZone n'est pas celle de
 *   java.time. La date lue (jour, mois, année) reste identique.
 */
class OCRDateExtractorEquivalenceTest {

    private val initialZone: TimeZone = TimeZone.getDefault()

    @After fun restore() = TimeZone.setDefault(initialZone)

    private val zones = listOf(
        "Europe/Paris", "UTC", "America/Sao_Paulo", "America/Santiago", "America/Havana", "Asia/Beirut",
        "Pacific/Apia", "Asia/Kolkata", "Australia/Lord_Howe", "America/New_York", "Pacific/Kiritimati"
    )

    private fun reference(line: String): Long? = try {
        OCRDateExtractorJavaTimeReference.extractDateMillis(listOf(line))
    } catch (e: ArithmeticException) {
        null
    }

    private fun structured(): List<String> {
        val days = listOf("00", "01", "09", "15", "28", "29", "30", "31", "32", "99", "1", "+1", "-1", "ab", "")
        val months = listOf("00", "01", "02", "04", "06", "09", "11", "12", "13", "1", "+1")
        val years = listOf("26", "99", "00", "2026", "2024", "2023", "2000", "2100", "1900", "1600", "1582", "0000",
            "0001", "0400", "+20261", "+00012", "+0001", "+2026", "-2026", "-0000", "20261", "202", "2", "",
            "+123456789", "+999999999", "+1000000000", "+292278993", "+300000000", "+9999999999999999999",
            "+00000000000000000001", "2026x", "26x", "2 0 2 6")
        val seps = listOf("/", "-", ".", ",", " ")
        val out = ArrayList<String>()
        for (d in days) for (m in months) for (y in years) {
            for (s1 in seps) for (s2 in seps) out += "$d$s1$m$s2$y"
        }
        for (base in listOf("12/01/2026", "31/04/2026", "29/02/2025", "30.02.24", "15-09-26")) {
            for (p in listOf("", "DATE:", "le ", "12:30 ", "TEL 01.23.45.67.89 ", "x")) for (s in listOf("", " 14:32", "x", "/", "1")) {
                out += "$p$base$s"
            }
        }
        return out
    }

    @Test fun equivalence_entrees_construites() {
        val inputs = structured()
        for (z in listOf("UTC")) {
            TimeZone.setDefault(TimeZone.getTimeZone(z))
            for (line in inputs) {
                assertEquals("$z « $line »", reference(line), OCRDateExtractor.extractDateMillisOriginalRules(listOf(line)))
            }
        }
    }

    @Test fun equivalence_entrees_aleatoires() {
        val alphabet = "0123456789/-.+ :ABx"
        val rnd = Random(20260930L)
        for (z in listOf("UTC")) {
            TimeZone.setDefault(TimeZone.getTimeZone(z))
            repeat(40_000) {
                val len = rnd.nextInt(17)
                val sb = StringBuilder()
                // moitié : gabarit de date perturbé ; moitié : caractères libres
                if (rnd.nextBoolean()) {
                    sb.append("%02d".format(rnd.nextInt(40))).append(alphabet[10 + rnd.nextInt(3)])
                        .append("%02d".format(rnd.nextInt(15))).append(alphabet[10 + rnd.nextInt(3)])
                        .append(if (rnd.nextBoolean()) "%02d".format(rnd.nextInt(100)) else "%04d".format(rnd.nextInt(10000)))
                    repeat(rnd.nextInt(3)) { sb.insert(rnd.nextInt(sb.length + 1), alphabet[rnd.nextInt(alphabet.length)]) }
                } else repeat(len) { sb.append(alphabet[rnd.nextInt(alphabet.length)]) }
                val line = sb.toString()
                assertEquals("$z « $line »", reference(line), OCRDateExtractor.extractDateMillisOriginalRules(listOf(line)))
            }
        }
    }

    /** Toutes les dates du 01/01/1901 au 31/12/2086, chaque fuseau : début du jour identique (changements d'heure à minuit compris). */
    @Test fun equivalence_debut_du_jour_1901_2086() {
        for (z in zones) {
            TimeZone.setDefault(TimeZone.getTimeZone(z))
            for (y in 1901..2086) for (m in 1..12) for (d in 1..31) {
                val line = "%02d/%02d/%04d".format(d, m, y)
                assertEquals("$z « $line »", reference(line), OCRDateExtractor.extractDateMillisOriginalRules(listOf(line)))
            }
        }
    }

    /** Hors plage de java.time.Instant en millisecondes : l'ancienne version plantait, la nouvelle ignore la ligne. */
    @Test fun annee_hors_plage_ligne_ignoree() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            OCRDateExtractorJavaTimeReference.extractDateMillis(listOf("12/01/+300000000"))
            throw AssertionError("java.time devait lever ArithmeticException")
        } catch (_: ArithmeticException) { }
        assertEquals(null, OCRDateExtractor.extractDateMillisOriginalRules(listOf("12/01/+300000000")))
        assertEquals(
            OCRDateExtractorJavaTimeReference.extractDateMillis(listOf("12/01/2026")),
            OCRDateExtractor.extractDateMillisOriginalRules(listOf("12/01/+300000000", "12/01/2026"))
        )
    }

    @Test fun plusieurs_lignes_premiere_datee() {
        val lines = listOf("SUPER U", "TEL 01.23.45.67.89", "31/04/2026 10:41", "12/01/2026")
        assertEquals(OCRDateExtractorJavaTimeReference.extractDateMillis(lines), OCRDateExtractor.extractDateMillisOriginalRules(lines))
    }
}
