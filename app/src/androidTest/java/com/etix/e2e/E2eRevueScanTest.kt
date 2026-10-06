package com.etix.e2e

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.etix.R
import com.etix.data.BudgetStore
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.e2e.ScanE2e.stubPicker
import com.etix.e2e.ScanE2e.ticketImage
import com.etix.e2e.ScanE2e.tickets
import com.etix.features.ocr.scan.MlKitTextReader
import com.etix.features.ocr.scan.ScanServices
import com.etix.features.ocr.scan.ScanTextReader
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.CompletableDeferred
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * Revue visuelle du scanner et du formulaire prérempli (captures seulement, aucun changement de l'app ni des règles OCR) :
 * émulateur en français, mêmes données fictives que les autres revues (E2eRevueAccueilTest), Clair puis Sombre, mêmes
 * images dans les deux thèmes. Argument facultatif `passe` (« petit » : 320 dp, police 2,0) ajouté aux noms.
 *
 * RÉEL : écrans du parcours (ScanFlowFragment), lecture ML Kit sur l'appareil, analyse (ReceiptScanParser), formulaire
 * « Ajouter » prérempli, clavier système. SIMULÉ : le sélecteur d'image (Espresso-Intents, renvoie une image de ticket
 * générée par le test, texte net, pas une photo) ; « lecture en cours » retenue par le test le temps de la capture (la
 * lecture ML Kit réelle reprend ensuite) ; « Lecture impossible » obtenue par un lecteur qui échoue (erreur simulée).
 * « Prendre une photo » n'est pas utilisé ici (appareil photo : E2eScanTest, E2eScanSystemeTest).
 *
 * Aucun appui sur « Enregistrer » : vérifié qu'aucun ticket n'est créé et que tickets et budgets sont inchangés.
 */
@RunWith(AndroidJUnit4::class)
class E2eRevueScanTest {

    private val instr get() = InstrumentationRegistry.getInstrumentation()
    private val passe: String? = InstrumentationRegistry.getArguments().getString("passe")

    // Images fictives, identiques dans les deux thèmes
    private val complet = listOf("ESSO", "12/01/2026", "TOTAL TTC 23,45 EUR", "CB VISA")
    private val partiel = listOf("BOULANGERIE MARTIN", "BAGUETTE 1,20", "CB 4,20")   // montant à vérifier, date absente

    private fun resumed() = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first()

    @Before fun setUp() { Intents.init() }

    @After fun tearDown() {
        Intents.release()
        ScanServices.reader = MlKitTextReader
    }

    private fun log(line: String) {
        val c = ctx.resources.configuration
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt").appendText(
            "scan $line sdk=${Build.VERSION.SDK_INT} largeur=${c.screenWidthDp}dp police=${c.fontScale}\n")
    }

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eRevueScanTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun chooseTheme(theme: String) {
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.rowTheme))
        E2e.chooseTheme(theme)
        waitFor(withId(R.id.rowTheme))
        pressBack()
        E2e.waitForAppReady("E2eRevueScanTest.$theme"); waitFor(withId(R.id.bottomNav))
    }

    private fun openScan() {
        onView(withId(R.id.menu_add)).perform(click())
        waitFor(withId(R.id.inputStore))
        onView(allOf(withId(R.id.btnScanTicket), isDisplayed())).perform(click())
        waitFor(withId(R.id.btnTakePhoto))
        SystemClock.sleep(600)                          // mise en page compacte éventuelle (petit écran)
    }

    /** Sélecteur d'image simulé renvoyant [lines] dessinées (même fichier pour les deux thèmes). */
    private fun pick(name: String, lines: List<String>, blank: Boolean = false) {
        Intents.release(); Intents.init()                 // un seul bouchon actif
        stubPicker(ticketImage(name, blank = blank, lines = lines))
        tap(R.id.btnPickImage)
    }

    /** Appui sur un bouton, après défilement s'il est dans le contenu défilant (mise en page compacte). */
    private fun tap(id: Int) {
        try { onView(withId(id)).perform(scrollTo()) } catch (_: Throwable) {}
        onView(withId(id)).perform(click())
    }

    private fun waitImeStable(): Int {
        var last = -1; var stable = 0
        val end = SystemClock.uptimeMillis() + 8000
        while (SystemClock.uptimeMillis() < end) {
            var h = 0
            instr.runOnMainSync {
                h = ViewCompat.getRootWindowInsets(resumed().window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            if (h > 0 && h == last) { if (++stable >= 2) return h } else stable = 0
            last = h
            SystemClock.sleep(250)
        }
        return last.coerceAtLeast(0)
    }

    /**
     * Bouton [id] amené à l'écran (défilement si besoin) puis entièrement visible entre la barre d'état et le haut du
     * clavier (ou le bas de l'écran). BLOQUANT : une action inaccessible n'est jamais seulement consignée.
     */
    private fun assertReachable(id: Int, label: String) {
        try { onView(withId(id)).perform(scrollTo()) } catch (_: Throwable) {}
        SystemClock.sleep(500)
        var line = ""; var ok = false
        instr.runOnMainSync {
            val act = resumed()
            val decor = act.window.decorView
            val ins = ViewCompat.getRootWindowInsets(decor)
            val ime = ins?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val bars = ins?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            val d = IntArray(2); decor.getLocationOnScreen(d)
            val v = act.findViewById<View>(id)
            val b = IntArray(2); v.getLocationOnScreen(b)
            val r = android.graphics.Rect()
            val top = d[1] + bars; val bottom = d[1] + decor.height - ime
            ok = v.isShown && v.getGlobalVisibleRect(r) && r.height() == v.height && r.width() == v.width &&
                b[1] >= top && b[1] + v.height <= bottom
            line = "$label : ${act.resources.getResourceEntryName(id)} [${b[1]},${b[1] + v.height}] zone [$top,$bottom] " +
                "clavier ${ime}px accessible=$ok"
        }
        log(line)
        assertTrue("Action accessible ($line)", ok)
    }

    private fun waitPrefill() = waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)

    private fun discardScan(s: String) {
        E2e.closeKeyboard()
        onView(withId(R.id.btnDiscardScan)).perform(scrollTo(), click())
        waitFor(allOf(withId(R.id.inputStore), withText("")))
        log("$s « Annuler le scan » : formulaire vidé")
    }

    private fun captureScan(theme: String, themeSuffix: String) {
        val s = if (passe == null) themeSuffix else "${themeSuffix}_$passe"
        val before = tickets(); val budgetsBefore = BudgetStore(ctx).load()
        startMain()
        chooseTheme(theme)

        // 1. Choix Photo / Image
        openScan()
        shot("revue_scan_1_choix_$s")
        for (id in listOf(R.id.btnTakePhoto, R.id.btnPickImage, R.id.btnScanCancel)) assertReachable(id, "$s choix")

        // 2. Lecture en cours (lecture ML Kit réelle retenue par le test le temps de la capture)
        val gate = CompletableDeferred<Unit>()
        ScanServices.reader = ScanTextReader { bmp -> gate.await(); MlKitTextReader.read(bmp) }
        pick("revue_complet.png", complet)
        waitFor(allOf(withId(R.id.stepProcessing), isDisplayed()))
        SystemClock.sleep(800)
        shot("revue_scan_2_lecture_$s")
        gate.complete(Unit)

        // 3. Résultat complet prérempli (rien n'est enregistré)
        waitPrefill()
        onView(withId(R.id.inputStore)).check(matches(withText("ESSO")))
        onView(withId(R.id.inputAmount)).check(matches(withText("23,45")))
        onView(withId(R.id.tvCategoryValue)).check(matches(withText("Carburant")))
        SystemClock.sleep(500)
        shot("revue_scan_3_complet_$s")
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo())
        SystemClock.sleep(400)
        shot("revue_scan_3b_complet_bas_$s")
        assertEquals("Aucun ticket créé sans « Enregistrer » ($s)", before, tickets())

        // 6. Clavier ouvert sur le montant : Enregistrer et « Annuler le scan » atteignables (sans les toucher)
        onView(withId(R.id.inputAmount)).perform(scrollTo(), click())
        var ime = waitImeStable()
        if (ime == 0) { log("$s clavier absent au 1er toucher, 2e toucher"); onView(withId(R.id.inputAmount)).perform(click()); ime = waitImeStable() }
        assertTrue("Clavier non affiché ($s)", ime > 0)
        shot("revue_scan_6_clavier_montant_$s")
        assertReachable(R.id.btnSaveTicket, "$s clavier ouvert")
        shot("revue_scan_6b_clavier_enregistrer_$s")
        assertReachable(R.id.btnDiscardScan, "$s clavier ouvert")
        shot("revue_scan_6c_clavier_annuler_scan_$s")
        assertEquals("Aucun ticket créé ($s)", before, tickets())
        discardScan(s)

        // 4. Informations absentes ou à vérifier (montant hors ligne de total, date absente)
        openScan()
        pick("revue_partiel.png", partiel)
        waitPrefill()
        onView(withId(R.id.badgeAmount)).check(matches(withText("À vérifier")))
        onView(withId(R.id.tvDateNote)).check(matches(withText("Date non lue — aujourd'hui proposé")))
        SystemClock.sleep(500)
        shot("revue_scan_4_a_verifier_$s")
        assertEquals(before, tickets())
        discardScan(s)
        // Montant absent : texte indicatif « Saisir le montant »
        openScan()
        pick("revue_sans_montant.png", listOf("MAGASIN DUPONT", "MERCI"))
        waitPrefill()
        onView(withId(R.id.badgeAmount)).check(matches(withText("Non lu")))
        onView(withId(R.id.inputAmount)).check(matches(withHint("Saisir le montant")))
        SystemClock.sleep(500)
        shot("revue_scan_4b_non_lu_$s")
        assertEquals(before, tickets())
        discardScan(s)

        // 5. Rien détecté (image blanche, lecture réelle) : Réessayer et Saisir manuellement
        openScan()
        pick("revue_blanc.png", emptyList(), blank = true)
        waitFor(withText("Aucune information détectée"), 60_000)
        SystemClock.sleep(600)
        shot("revue_scan_5_rien_detecte_$s")
        for (id in listOf(R.id.btnRetry, R.id.btnManualEntry)) assertReachable(id, "$s rien détecté")
        tap(R.id.btnRetry)
        waitFor(allOf(withId(R.id.btnTakePhoto), isDisplayed()))
        // 5b. Erreur de lecture (simulée : lecteur en échec)
        ScanServices.reader = ScanTextReader { throw IllegalStateException("erreur simulée pour la revue") }
        pick("revue_complet.png", complet)
        waitFor(withText("Lecture impossible"), 30_000)
        SystemClock.sleep(600)
        shot("revue_scan_5b_erreur_$s")
        for (id in listOf(R.id.btnRetry, R.id.btnManualEntry)) assertReachable(id, "$s erreur")
        ScanServices.reader = MlKitTextReader
        tap(R.id.btnManualEntry)
        waitFor(allOf(withId(R.id.inputStore), withText("")))
        log("$s « Saisir manuellement » : formulaire vide, aucun ticket")

        assertEquals("Aucun ticket créé ni modifié ($s)", before, tickets())
        assertEquals("Budgets inchangés ($s)", budgetsBefore, BudgetStore(ctx).load())
    }

    @Test fun e01_scanner_clair_puis_sombre() {
        assertEquals("Émulateur attendu en français", "fr", Locale.getDefault().language)
        assertTrue("Mesure du clavier (insets) : API 30+", Build.VERSION.SDK_INT >= 30)
        val before = tickets(); val budgetsBefore = BudgetStore(ctx).load()
        assertEquals("Données de la revue de l'Accueil attendues (11 tickets fictifs)", 11, before.size)
        captureScan("Clair", "clair")
        captureScan("Sombre", "sombre")
        startMain()
        chooseTheme("Système")
        assertEquals("Aucun ticket créé ni modifié", before, tickets())
        assertEquals(budgetsBefore, BudgetStore(ctx).load())
    }
}
