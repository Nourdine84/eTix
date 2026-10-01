package com.etix.e2e

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.e2e.E2e.ctx
import com.etix.features.ocr.engine.OCRDateExtractor
import com.etix.features.ocr.engine.OCRProcessor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Calendar

/**
 * Compatibilité Android : lecteur de dates OCR exécuté sur l'appareil (API 21 à 36).
 *
 * Ne touche ni aux données ni à l'interface (logique pure). Le scanner reste désactivé :
 * le test appelle directement OCRDateExtractor et OCRProcessor.
 *
 * Attendus = comportement actuel des règles OCR, relevé sur la JVM (voir OCRDateExtractorTest,
 * même table). Chaque résultat, ou l'erreur levée, est consigné dans shots/compat_dates.txt.
 */
@RunWith(AndroidJUnit4::class)
class E2eCompatDatesOcrTest {

    private fun day(y: Int, m: Int, d: Int): Long = Calendar.getInstance().apply {
        clear(); set(y, m - 1, d, 0, 0, 0)
    }.timeInMillis

    private fun log(s: String) =
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "compat_dates.txt").appendText(s + "\n")

    private fun cases(): List<Pair<List<String>, Long?>> = listOf(
        listOf("CARREFOUR", "12/01/2026", "TOTAL 10,00") to day(2026, 1, 12),
        listOf("DATE: 15-09-2026 14:32") to day(2026, 9, 15),
        listOf("15.09.26") to day(2026, 9, 15),
        listOf("12 / 01 / 2026") to day(2026, 1, 12),
        listOf("TEL 01.23.45.67.89", "03/10/2026") to day(2026, 10, 3),
        listOf("29/02/2024") to day(2024, 2, 29),
        listOf("31/02/2026") to day(2026, 2, 28),
        listOf("32/01/2026") to null,
        listOf("12/13/2026") to null,
        listOf("1/2/2026") to null,
        listOf("pas de date") to null
    )

    @Test
    fun c01_lecteur_de_dates_sur_l_appareil() {
        log("API ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})")
        val failures = mutableListOf<String>()
        for ((lines, expected) in cases()) {
            val got: Result<Long?> = try {
                Result.success(OCRDateExtractor.extractDateMillis(lines))
            } catch (t: Throwable) { // NoClassDefFoundError, NoSuchMethodError… : consignés puis signalés
                Result.failure(t)
            }
            val line = got.fold(
                onSuccess = { v -> if (v == expected) "OK   $lines -> $v" else "ÉCART $lines -> $v (attendu $expected)" },
                onFailure = { t -> "ERREUR $lines -> ${t.javaClass.name}: ${t.message}" }
            )
            log("  $line")
            if (!line.startsWith("OK")) failures += line
        }
        assertEquals("Lecteur de dates OCR sur API ${Build.VERSION.SDK_INT}", emptyList<String>(), failures)
    }

    @Test
    fun c02_traitement_ocr_complet_sur_l_appareil() {
        val raw = "SUPER U\n12/01/2026 10:41\nSOUS-TOTAL 9,00\nTOTAL A PAYER 12,50\nCB 12,50"
        val r = try { OCRProcessor.process(raw) } catch (t: Throwable) {
            log("  ERREUR OCRProcessor.process -> ${t.javaClass.name}: ${t.message}")
            throw t
        }
        log("  OCRProcessor.process -> montant=${r.amount} date=${r.dateMillis}")
        assertEquals(day(2026, 1, 12), r.dateMillis)
        assertEquals(12.5, r.amount!!, 0.001)
    }
}
