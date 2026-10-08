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
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
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
@org.junit.FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
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
        waitFor(withId(R.id.scrollViewAdd))             // page Ajouter (le champ magasin peut être hors écran en grande police)
        // Bouton de la page « Ajouter » (l'Accueil porte le même identifiant), ramené à l'écran : après « Annuler le scan »
        // le formulaire peut rester défilé
        onView(allOf(withId(R.id.btnScanTicket), isDescendantOfA(withId(R.id.scrollViewAdd)))).perform(scrollTo(), click())
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
     * Vue entière amenée à l'écran par son parent défilant (requestRectangleOnScreen sur toute la vue). Le scrollTo()
     * d'Espresso ne fait rien dès que 90 % de la vue sont visibles : une dernière ligne coupée resterait coupée.
     */
    private fun bringFullyOnScreen(id: Int, inOverlay: Boolean) {
        instr.runOnMainSync {
            val act = resumed()
            val root: View = if (inOverlay) act.findViewById(R.id.overlayContainer) else act.window.decorView
            val v = root.findViewById<View>(id) ?: return@runOnMainSync
            v.requestRectangleOnScreen(android.graphics.Rect(0, 0, v.width, v.height), true)
        }
        SystemClock.sleep(600)
    }

    /**
     * Bouton [id] amené à l'écran (défilement si besoin) puis entièrement visible entre la barre d'état et le haut du
     * clavier (ou le bas de l'écran). BLOQUANT : une action inaccessible n'est jamais seulement consignée.
     */
    private fun assertReachable(id: Int, label: String) {
        bringFullyOnScreen(id, false)
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

    /**
     * Texte LISIBLE, pas seulement présent (BLOQUANT) : vue amenée à l'écran (défilement si besoin), zone du texte
     * (lignes, marges intérieures exclues) entièrement visible entre la barre d'état et le clavier ou le bas de la zone
     * utile (barre d'actions exclue, le parent défilant la découpe), aucune ligne tronquée par « … », aucun mot coupé en fin de ligne, tout le texte dans la hauteur de la
     * vue et aucun défilement interne caché (champ d'une ligne qui ne montrerait que la fin ou le début du nom).
     * [singleLine] : le libellé doit tenir sur une ligne.
     */
    private fun assertReadable(id: Int, label: String, singleLine: Boolean = false, inOverlay: Boolean = false) {
        bringFullyOnScreen(id, inOverlay)
        var line = ""; val problems = mutableListOf<String>()
        instr.runOnMainSync {
            val act = resumed()
            val decor = act.window.decorView
            val ins = ViewCompat.getRootWindowInsets(decor)
            val ime = ins?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val bars = ins?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            val d = IntArray(2); decor.getLocationOnScreen(d)
            val root: View = if (inOverlay) act.findViewById(R.id.overlayContainer) else decor
            val v = root.findViewById<android.widget.TextView>(id)
            val loc = IntArray(2); v.getLocationOnScreen(loc)
            val r = android.graphics.Rect()
            val top = d[1] + bars; val bottom = d[1] + decor.height - ime
            val text = v.text.toString()
            val l = v.layout
            // Zone du TEXTE (marges intérieures exclues : une marge rognée ne gêne pas la lecture), coordonnées fenêtre
            val win = IntArray(2); v.getLocationInWindow(win)
            val tTop = v.totalPaddingTop; val tBottom = v.totalPaddingTop + (l?.height ?: 0)
            val shown = v.isShown && v.getGlobalVisibleRect(r)
            if (!shown || r.top > win[1] + tTop || r.bottom < win[1] + tBottom ||
                r.left > win[0] + v.totalPaddingLeft || r.right < win[0] + v.width - v.totalPaddingRight)
                problems += "texte partiellement masqué (visible [${r.top},${r.bottom}], texte [${win[1] + tTop},${win[1] + tBottom}])"
            if (loc[1] + tTop < top || loc[1] + tBottom > bottom) problems += "texte hors zone [$top,$bottom]"
            if (l == null) problems += "texte non mis en page" else {
                for (i in 0 until l.lineCount) if (l.getEllipsisCount(i) > 0) problems += "ligne ${i + 1} tronquée"
                for (i in 0 until l.lineCount - 1) {
                    val end = l.getLineEnd(i)
                    val before = text.getOrNull(end - 1); val after = text.getOrNull(end)
                    val wordCut = before != null && after != null && !before.isWhitespace() && !after.isWhitespace() &&
                        before !in "-/’'" && before.isLetterOrDigit() && after.isLetterOrDigit()
                    if (wordCut) problems += "mot coupé après « ${text.substring(l.getLineStart(i), end)} »"
                }
                val inner = v.height - v.totalPaddingTop - v.totalPaddingBottom
                if (l.height > inner + 1) problems += "texte plus haut que la vue (${l.height}/${inner}px)"
                if (v.scrollX != 0 || v.scrollY != 0) problems += "défilement interne (${v.scrollX},${v.scrollY})"
                if (singleLine && l.lineCount > 1) problems += "${l.lineCount} lignes au lieu d'une"
            }
            line = "$label : ${act.resources.getResourceEntryName(id)} « ${text.replace('\n', '⏎')} » " +
                "${l?.lineCount ?: 0} ligne(s) [${loc[1]},${loc[1] + v.height}] zone [$top,$bottom] " +
                (if (problems.isEmpty()) "lisible" else "ILLISIBLE : ${problems.joinToString(" ; ")}")
        }
        log(line)
        assertTrue("Texte lisible ($line)", problems.isEmpty())
    }

    private fun waitPrefill() = waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)

    /** Formulaire vidé (champ magasin vide), sans exiger qu'il soit à l'écran (grande police : il peut être plus bas). */
    private fun waitEmptyForm(label: String) {
        val end = SystemClock.uptimeMillis() + 10_000
        var text: String? = null
        while (SystemClock.uptimeMillis() < end) {
            instr.runOnMainSync { text = resumed().findViewById<android.widget.TextView>(R.id.inputStore)?.text?.toString() }
            if (text == "") return
            SystemClock.sleep(200)
        }
        throw AssertionError("Formulaire non vidé ($label) : magasin « $text »")
    }

    private fun discardScan(s: String) {
        E2e.closeKeyboard()
        onView(withId(R.id.btnDiscardScan)).perform(scrollTo(), click())
        waitEmptyForm("$s, Annuler le scan")
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
        // Texte d'introduction : dernière ligne accessible par défilement, au-dessus de la barre d'actions
        assertReadable(R.id.tvScanIntro, "$s introduction")
        shot("revue_scan_1b_choix_texte_$s")
        assertReachable(R.id.btnTakePhoto, "$s choix après défilement")

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
        // Libellé « CATÉGORIE » sur une ligne et indication « Suggéré par l'OCR » lisible
        assertReadable(R.id.tvCategoryLabel, "$s catégorie", singleLine = true)
        assertReadable(R.id.tvCategorySuggested, "$s indication OCR")
        assertReadable(R.id.tvCategoryValue, "$s catégorie (valeur)")
        shot("revue_scan_3c_categorie_$s")
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
        assertReadable(R.id.inputStore, "$s magasin lu")
        // Mention de date proposée : lisible après défilement
        assertReadable(R.id.tvDateNote, "$s date non lue")
        shot("revue_scan_4d_date_non_lue_$s")
        // Nom modifiable : ajout en fin de nom, aucun saut de ligne même avec la touche Entrée
        var lu = ""
        instr.runOnMainSync {
            val f = resumed().findViewById<android.widget.EditText>(R.id.inputStore)
            lu = f.text.toString(); f.requestFocus(); f.setSelection(f.text.length)
        }
        onView(withId(R.id.inputStore)).perform(androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView(" ET FILS"))
        onView(withId(R.id.inputStore)).perform(androidx.test.espresso.action.ViewActions.pressKey(android.view.KeyEvent.KEYCODE_ENTER))
        SystemClock.sleep(500)
        var modifie = ""
        instr.runOnMainSync { modifie = resumed().findViewById<android.widget.EditText>(R.id.inputStore).text.toString() }
        log("$s magasin modifié : « $lu » → « ${modifie.replace("\n", "⏎")} »")
        assertEquals("Nom modifié exactement, sans saut de ligne ($s)", "$lu ET FILS", modifie)
        assertReadable(R.id.inputStore, "$s magasin modifié")
        shot("revue_scan_4c_nom_modifie_$s")
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
        assertReadable(R.id.inputStore, "$s magasin lu")
        assertEquals(before, tickets())
        discardScan(s)

        // 5. Rien détecté (image blanche, lecture réelle) : Réessayer et Saisir manuellement
        openScan()
        pick("revue_blanc.png", emptyList(), blank = true)
        waitFor(withText("Aucune information détectée"), 60_000)
        SystemClock.sleep(600)
        shot("revue_scan_5_rien_detecte_$s")
        for (id in listOf(R.id.btnRetry, R.id.btnManualEntry)) assertReachable(id, "$s rien détecté")
        assertReadable(R.id.tvFailureTitle, "$s rien détecté (titre)")
        assertReadable(R.id.tvFailureMessage, "$s rien détecté (message)")
        shot("revue_scan_5c_rien_detecte_texte_$s")
        tap(R.id.btnRetry)
        waitFor(allOf(withId(R.id.btnTakePhoto), isDisplayed()))
        // 5b. Erreur de lecture (simulée : lecteur en échec)
        ScanServices.reader = ScanTextReader { throw IllegalStateException("erreur simulée pour la revue") }
        pick("revue_complet.png", complet)
        waitFor(withText("Lecture impossible"), 30_000)
        SystemClock.sleep(600)
        shot("revue_scan_5b_erreur_$s")
        for (id in listOf(R.id.btnRetry, R.id.btnManualEntry)) assertReachable(id, "$s erreur")
        assertReadable(R.id.tvFailureTitle, "$s erreur (titre)")
        assertReadable(R.id.tvFailureMessage, "$s erreur (message)")
        shot("revue_scan_5d_erreur_texte_$s")
        ScanServices.reader = MlKitTextReader
        tap(R.id.btnManualEntry)
        waitFor(withId(R.id.bottomNav))
        waitEmptyForm("$s, Saisir manuellement")
        log("$s « Saisir manuellement » : formulaire vide, aucun ticket")

        assertEquals("Aucun ticket créé ni modifié ($s)", before, tickets())
        assertEquals("Budgets inchangés ($s)", budgetsBefore, BudgetStore(ctx).load())
    }

    /**
     * Modifier partage le formulaire : nom long entièrement lisible et modifiable, libellé « CATÉGORIE » sur une ligne ;
     * sortie par retour arrière, rien d'enregistré (thème Système).
     */
    @Test fun e02_modifier_nom_long() {
        val s = if (passe == null) "modifier" else "modifier_$passe"
        val before = tickets(); val budgetsBefore = BudgetStore(ctx).load()
        startMain()
        onView(withId(R.id.menu_history)).perform(click())
        waitFor(withId(R.id.recyclerHistory))
        onView(withId(R.id.recyclerHistory)).perform(
            androidx.test.espresso.contrib.RecyclerViewActions.scrollTo<androidx.recyclerview.widget.RecyclerView.ViewHolder>(
                androidx.test.espresso.matcher.ViewMatchers.hasDescendant(withText("Réseau de bus"))))
        onView(allOf(withText("Réseau de bus"), isDescendantOfA(withId(R.id.recyclerHistory)), isDisplayed())).perform(click())
        val overlay = { id: Int -> allOf(withId(id), isDescendantOfA(withId(R.id.overlayContainer))) }
        waitFor(overlay(R.id.tvAmount))
        onView(overlay(R.id.btnEdit)).perform(E2e.nestedScrollTo(), click())
        waitFor(overlay(R.id.inputStore))
        val long = "Boulangerie Pâtisserie des Halles du Centre-Ville"
        onView(overlay(R.id.inputStore)).perform(androidx.test.espresso.action.ViewActions.replaceText(long))
        E2e.closeKeyboard()
        var shown = ""
        instr.runOnMainSync {
            shown = resumed().findViewById<View>(R.id.overlayContainer).findViewById<android.widget.EditText>(R.id.inputStore).text.toString()
        }
        assertEquals("Valeur saisie conservée exactement ($s)", long, shown)
        assertReadable(R.id.inputStore, "$s nom long", inOverlay = true)
        shot("revue_scan_7_modifier_nom_long_$s")
        assertReadable(R.id.tvCategoryLabel, "$s catégorie", singleLine = true, inOverlay = true)
        pressBack()                                   // quitte la modification sans enregistrer
        SystemClock.sleep(800)
        pressBack()
        waitFor(withId(R.id.bottomNav))
        assertEquals("Aucun ticket modifié ($s)", before, tickets())
        assertEquals(budgetsBefore, BudgetStore(ctx).load())
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
