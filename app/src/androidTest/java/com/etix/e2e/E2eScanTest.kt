package com.etix.e2e

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.etix.R
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.e2e.ScanE2e.asset
import com.etix.e2e.ScanE2e.stubCamera
import com.etix.e2e.ScanE2e.stubPicker
import com.etix.e2e.ScanE2e.ticketImage
import com.etix.e2e.ScanE2e.tickets
import com.etix.e2e.ScanE2e.uriOf
import com.etix.features.ocr.scan.MlKitTextReader
import com.etix.features.ocr.scan.ReceiptScanParser
import com.etix.features.ocr.scan.ScanImageLoader
import com.etix.features.ocr.scan.ScanServices
import com.etix.features.ocr.scan.ScanTextReader
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.Calendar

/**
 * Lot 9 — parcours de scan sur émulateur, reconnaissance ML Kit RÉELLE sur des images de ticket générées.
 * Simulés (Espresso-Intents) : l'application appareil photo et le sélecteur d'image, qui renvoient l'image comme
 * le ferait l'utilisateur (parcours réels avec les applications du système : E2eScanSystemeTest).
 * Données fictives ; tickets créés uniquement après « Enregistrer » ; tickets existants jamais modifiés.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eScanTest {

    private val instr get() = InstrumentationRegistry.getInstrumentation()

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eScanTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun openScanFromAdd() {
        onView(withId(R.id.menu_add)).perform(click())
        waitFor(withId(R.id.inputStore))
        onView(allOf(withId(R.id.btnScanTicket), isDisplayed())).perform(click())
        waitFor(withId(R.id.btnPickImage))
    }

    /** Formulaire « Ajouter » prérempli par le scan (ML Kit réel), en 60 s maximum (1re initialisation ML Kit). */
    private fun waitForPrefill() {
        waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)
        // Le formulaire n'existe que sur la page « Ajouter » : identifiants uniques, sans exiger l'affichage
        // (la catégorie peut être sous le bord de l'écran)
        onView(withId(R.id.inputStore)).check(matches(withText("ESSO")))
        onView(withId(R.id.inputAmount)).check(matches(withText("23,45")))
        onView(withId(R.id.tvCategoryValue)).check(matches(withText("Carburant")))
        // 02/10/2026 : jamais « Vérifié » pour une valeur lue
        onView(withId(R.id.badgeAmount)).check(matches(withText("Détecté")))
        onView(withId(R.id.badgeDate)).check(matches(withText("Détectée")))
        onView(withId(R.id.badgeStore)).check(matches(withText("À vérifier")))
    }

    private fun resumed(): Activity {
        var a: Activity? = null
        instr.runOnMainSync {
            a = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return a ?: throw AssertionError("aucune activité au premier plan")
    }

    /** Rotation réelle de l'écran (recréation de l'activité), puis attente de la nouvelle activité. */
    private fun rotate(orientation: Int) {
        val before = resumed()
        instr.runOnMainSync { before.requestedOrientation = orientation }
        val end = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < end) {
            instr.waitForIdleSync()
            val now = runCatching { resumed() }.getOrNull()
            if (now != null && now !== before) break
            SystemClock.sleep(200)
        }
        E2e.waitForAppReady("E2eScanTest rotation")
    }

    private fun waitTickets(count: Int, timeoutMs: Long = 10_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (tickets().size < count && System.currentTimeMillis() < end) Thread.sleep(200)
    }

    private fun effectivelyVisible(id: Int) = onView(withId(id)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    private fun gone(id: Int) = onView(withId(id)).check(matches(not(withEffectiveVisibility(Visibility.VISIBLE))))

    @Before fun setUp() { Intents.init() }

    @After fun tearDown() {
        Intents.release()
        ScanServices.reader = MlKitTextReader
        runCatching { resumed() }.getOrNull()?.let { a ->
            instr.runOnMainSync { a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    /** Texte lu par ML Kit sur l'image de test, consigné pour le diagnostic (shots/scan_ocr.txt). */
    @Test fun s00_mlkit_lit_l_image_de_test() {
        val bmp = BitmapFactory.decodeFile(ticketImage("diag.png").path)
        val text = runBlocking { MlKitTextReader.read(bmp) }
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "scan_ocr.txt")
            .appendText("API ${Build.VERSION.SDK_INT} — ML Kit :\n$text\n")
        assertEquals(true, text.contains("ESSO") && text.contains("23,45"))
    }

    @Test fun s01_image_choisie_formulaire_prerempli_puis_annuler_sans_ticket() {
        val before = tickets()
        startMain()
        openScanFromAdd()
        gone(R.id.bottomNav)                           // plein écran pendant le scan (iOS fullScreenCover)
        shot("70_scan_intro")
        stubPicker(ticketImage("galerie.png"))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitForPrefill()
        effectivelyVisible(R.id.bottomNav)
        shot("71_scan_formulaire_prerempli")
        assertEquals(before, tickets())
        onView(withId(R.id.btnDiscardScan)).perform(click())
        waitFor(allOf(withId(R.id.inputStore), withText("")))
        assertEquals(before, tickets())
    }

    /** « Prendre une photo » : aucune demande d'autorisation, l'appareil photo (simulé ici) est ouvert directement. */
    @Test fun s02_photo_depuis_l_accueil_puis_enregistrement_apres_validation() {
        val before = tickets()
        startMain()
        onView(withId(R.id.menu_home)).perform(click())
        onView(allOf(withId(R.id.btnScanTicket), androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA(withId(R.id.homeScroll))))
            .perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.btnTakePhoto))
        stubCamera(ticketImage("photo.png"))
        onView(withId(R.id.btnTakePhoto)).perform(click())
        waitForPrefill()
        assertEquals("Aucun ticket avant « Enregistrer »", before, tickets())
        onView(allOf(withId(R.id.inputAmount), isDisplayed())).perform(replaceText("23,40"))
        E2e.closeKeyboard()
        gone(R.id.badgeAmount)                         // valeur corrigée : plus marquée comme détectée
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())
        waitTickets(before.size + 1)
        val after = tickets()
        assertEquals(before, after.take(before.size))           // tickets existants inchangés
        assertEquals(before.size + 1, after.size)
        val t = after.last()
        assertEquals("ESSO", t.store); assertEquals(23.40, t.amount, 0.001); assertEquals("Carburant", t.category)
        val c = Calendar.getInstance().apply { timeInMillis = t.dateMillis }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
        assertEquals(2026, c.get(Calendar.YEAR))
        shot("72_scan_enregistre")
    }

    @Test fun s03_rien_detecte_reessayer_puis_retour_sans_ticket() {
        val before = tickets()
        startMain()
        openScanFromAdd()
        stubPicker(ticketImage("vide.png", blank = true))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitFor(withText("Aucune information détectée"), 60_000)
        shot("73_scan_rien_detecte")
        onView(withId(R.id.btnRetry)).perform(click())
        waitFor(withId(R.id.btnPickImage))
        pressBack()
        waitFor(withId(R.id.inputStore))
        assertEquals(before, tickets())
    }

    /** Double appui très rapproché sur « Enregistrer » (deux clics dans le même passage du thread principal). */
    @Test fun s04_double_appui_enregistrer_un_seul_ticket() {
        val before = tickets()
        startMain()
        openScanFromAdd()
        stubPicker(ticketImage("double.png"))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitForPrefill()
        val activity = resumed()
        instr.runOnMainSync {
            val b = activity.findViewById<View>(R.id.btnSaveTicket)
            b.performClick(); b.performClick()
        }
        waitTickets(before.size + 1)
        Thread.sleep(2_000)                            // un éventuel 2e enregistrement aurait eu le temps d'aboutir
        val after = tickets()
        assertEquals("un seul ticket pour un double appui", before.size + 1, after.size)
        assertEquals(before, after.take(before.size))
        waitFor(allOf(withId(R.id.inputStore), withText("")))
    }

    /** Retour système pendant la lecture : la lecture (ML Kit réel, retenue par le test) se termine ensuite sans effet. */
    @Test fun s05_annulation_pendant_la_lecture() {
        val before = tickets()
        val gate = CompletableDeferred<Unit>()
        ScanServices.reader = ScanTextReader { bmp -> gate.await(); MlKitTextReader.read(bmp) }
        startMain()
        openScanFromAdd()
        stubPicker(ticketImage("annulation.png"))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitFor(withId(R.id.stepProcessing))
        shot("74_scan_lecture_en_cours")
        pressBack()
        waitFor(withId(R.id.inputStore))
        gate.complete(Unit)
        Thread.sleep(4_000)                            // fin de la reconnaissance après l'annulation
        instr.waitForIdleSync()
        onView(withId(R.id.inputStore)).check(matches(withText("")))
        onView(withId(R.id.inputAmount)).check(matches(withText("")))
        gone(R.id.scanBanner)
        // parcours de scan fermé (retiré de l'écran) : aucune étape de lecture réaffichée
        onView(withId(R.id.stepProcessing)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
        assertEquals(before, tickets())
    }

    /** Rotation pendant la lecture puis après correction : rien perdu, rien en double, un seul ticket à la validation. */
    @Test fun s06_rotation_pendant_la_lecture_et_apres_correction() {
        val before = tickets()
        val gate = CompletableDeferred<Unit>()
        ScanServices.reader = ScanTextReader { bmp -> gate.await(); MlKitTextReader.read(bmp) }
        startMain()
        openScanFromAdd()
        stubPicker(ticketImage("rotation.png"))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitFor(withId(R.id.stepProcessing))
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        waitFor(withId(R.id.stepProcessing))           // la lecture continue après recréation
        gate.complete(Unit)
        waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)
        onView(withId(R.id.inputStore)).check(matches(withText("ESSO")))
        shot("75_scan_paysage_prerempli")
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        waitForPrefill()
        onView(allOf(withId(R.id.inputAmount), isDisplayed())).perform(replaceText("23,40"))
        E2e.closeKeyboard()
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        waitFor(allOf(withId(R.id.scanBanner), isDisplayed()))
        onView(withId(R.id.inputStore)).check(matches(withText("ESSO")))
        onView(withId(R.id.inputAmount)).check(matches(withText("23,40")))
        onView(withId(R.id.tvCategoryValue)).check(matches(withText("Carburant")))
        onView(withId(R.id.badgeStore)).check(matches(withText("À vérifier")))
        effectivelyVisible(R.id.badgeStore)
        gone(R.id.badgeAmount)
        onView(withId(R.id.badgeDate)).check(matches(withText("Détectée")))
        assertEquals("aucun ticket avant « Enregistrer »", before, tickets())
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())
        waitTickets(before.size + 1)
        Thread.sleep(1_000)
        val after = tickets()
        assertEquals(before.size + 1, after.size)
        val t = after.last()
        assertEquals("ESSO", t.store); assertEquals(23.40, t.amount, 0.001)
        val c = Calendar.getInstance().apply { timeInMillis = t.dateMillis }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
    }

    /** Photo dont l'orientation n'est donnée que par ses métadonnées EXIF (6 = 90°) : redressée avant lecture. */
    @Test fun s07_image_orientee_par_exif() {
        val f = asset("exif_orientation_6.jpg")
        val bmp = ScanImageLoader.load(ctx, uriOf(f))
        ScanE2e.log("scan_images", "EXIF 6 : fichier 1500x1080 → ${bmp.width}x${bmp.height}")
        assertEquals(1080, bmp.width); assertEquals(1500, bmp.height)
        val before = tickets()
        startMain()
        openScanFromAdd()
        stubPicker(f)
        onView(withId(R.id.btnPickImage)).perform(click())
        waitForPrefill()
        assertEquals(before, tickets())
    }

    /** Très grande image (48 Mpx) : réduite au budget sans erreur mémoire, puis lue. */
    @Test fun s08_image_volumineuse() {
        val f = asset("grande_6000x8000.jpg")
        val t0 = SystemClock.uptimeMillis()
        val bmp = ScanImageLoader.load(ctx, uriOf(f))
        val px = bmp.width.toLong() * bmp.height
        val rt = Runtime.getRuntime()
        ScanE2e.log("scan_images", "6000x8000 → ${bmp.width}x${bmp.height} ($px px) en ${SystemClock.uptimeMillis() - t0} ms ; " +
            "tas max ${rt.maxMemory() / 1_048_576} Mo")
        assertTrue("$px px", px <= ScanImageLoader.MAX_PIXELS && px >= ScanImageLoader.MAX_PIXELS * 9 / 10)
        bmp.recycle()
        val before = tickets()
        startMain()
        openScanFromAdd()
        stubPicker(f)
        onView(withId(R.id.btnPickImage)).perform(click())
        waitForPrefill()
        assertEquals(before, tickets())
    }

    /** Ticket long (1000 × 7000) : largeur préservée, total en bas du ticket lu. */
    @Test fun s09_ticket_long() {
        val f = asset("ticket_long_1000x7000.jpg")
        val bmp = ScanImageLoader.load(ctx, uriOf(f))
        val text = runBlocking { MlKitTextReader.read(bmp) }
        val scan = ReceiptScanParser.parse(text)
        ScanE2e.log("scan_images", "ticket long 1000x7000 → ${bmp.width}x${bmp.height} ; magasin=${scan.store.value} " +
            "montant=${scan.amount.value} (${scan.amount.confidence}) date lue=${scan.date.value != null}")
        assertTrue("largeur ${bmp.width}", bmp.width >= 700)
        assertEquals("ESSO", scan.store.value)
        assertEquals(23.45, scan.amount.value!!, 0.001)
    }

    /**
     * Ticket sans montant ni date (ML Kit réel) : « Non lu », texte indicatif « Saisir le montant », mention « Date non
     * lue — aujourd'hui proposé » ; « Enregistrer » refusé (aucun ticket, aucun montant 0), puis accepté une fois le
     * montant saisi.
     */
    @Test fun s10_champs_non_lus_et_montant_obligatoire() {
        val before = tickets()
        startMain()
        openScanFromAdd()
        stubPicker(ticketImage("sans_montant.png", lines = listOf("MAGASIN DUPONT", "MERCI")))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)
        onView(withId(R.id.inputAmount)).check(matches(withText("")))
        onView(withId(R.id.inputAmount)).check(matches(androidx.test.espresso.matcher.ViewMatchers.withHint("Saisir le montant")))
        onView(withId(R.id.badgeAmount)).check(matches(withText("Non lu")))
        onView(withId(R.id.tvDateNote)).check(matches(withText("Date non lue — aujourd'hui proposé")))
        effectivelyVisible(R.id.tvDateNote)
        gone(R.id.badgeDate)
        shot("76_scan_champs_non_lus")
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())
        Thread.sleep(1_500)
        assertEquals("montant vide : aucun ticket", before, tickets())
        onView(withId(R.id.scanBanner)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.inputAmount)).perform(scrollTo(), replaceText("7,50"))
        E2e.closeKeyboard()
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())
        waitTickets(before.size + 1)
        val after = tickets()
        assertEquals(before.size + 1, after.size)
        assertEquals(7.50, after.last().amount, 0.001)
    }
}
