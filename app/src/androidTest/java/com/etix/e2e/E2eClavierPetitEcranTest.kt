package com.etix.e2e

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

/**
 * Accessibilité du bouton d'enregistrement CLAVIER OUVERT, petit écran et grande police (lot 6).
 * Espresso `isDisplayed` ignore la fenêtre du clavier : on MESURE donc la position du bouton par rapport au haut du
 * clavier (insets IME, API 30+), puis on touche réellement le bouton et on vérifie l'effet (ticket enregistré).
 * Passe (taille d'écran, densité, police) fixée par le script CI ; argument d'instrumentation « passe ».
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eClavierPetitEcranTest {

    private val instr get() = InstrumentationRegistry.getInstrumentation()
    private val passe: String get() = InstrumentationRegistry.getArguments().getString("passe", "x")
    private val store get() = "Clavier $passe"

    @Before fun session() {
        assumeTrue("Mesure des insets clavier : API 30+", Build.VERSION.SDK_INT >= 30)
        SessionManager(ctx).apply { markFirstLaunchDone(); login("Testeur clavier") }
    }

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eClavierPetitEcranTest"); waitFor(withId(R.id.bottomNav))
    }

    /** À appeler SUR le thread principal. */
    private fun resumedOnMain(): Activity =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first()

    /** Hauteur du clavier en px, stable sur deux lectures (fin d'animation) ; 0 si absent. */
    private fun waitImeStable(): Int {
        var last = -1; var stable = 0
        val end = SystemClock.uptimeMillis() + 8000
        while (SystemClock.uptimeMillis() < end) {
            var h = 0
            instr.runOnMainSync {
                h = ViewCompat.getRootWindowInsets(resumedOnMain().window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            if (h > 0 && h == last) { if (++stable >= 2) return h } else stable = 0
            last = h
            SystemClock.sleep(250)
        }
        return last.coerceAtLeast(0)
    }

    private data class Mesure(val top: Int, val bottom: Int, val haut: Int, val clavierHaut: Int, val clavier: Int) {
        val visible get() = clavier > 0 && top >= haut && bottom <= clavierHaut
    }

    private fun mesure(buttonId: Int): Mesure {
        var m: Mesure? = null
        instr.runOnMainSync {
            val act = resumedOnMain()
            val decor = act.window.decorView
            val ins = ViewCompat.getRootWindowInsets(decor)
            val ime = ins?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val bars = ins?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            val d = IntArray(2).also(decor::getLocationOnScreen)
            val b = IntArray(2).also(act.findViewById<View>(buttonId)::getLocationOnScreen)
            val v = act.findViewById<View>(buttonId)
            m = Mesure(b[1], b[1] + v.height, d[1] + bars, d[1] + decor.height - ime, ime)
        }
        return m!!
    }

    private fun log(ecran: String, avant: Mesure, apres: Mesure) {
        val res = ctx.resources
        val dm = res.displayMetrics
        val line = "passe=$passe sdk=${Build.VERSION.SDK_INT} police=${res.configuration.fontScale} " +
            "ecran=${(dm.widthPixels / dm.density).toInt()}x${(dm.heightPixels / dm.density).toInt()}dp " +
            "$ecran clavier=${apres.clavier}px haut_clavier=${apres.clavierHaut} " +
            "bouton_avant=[${avant.top},${avant.bottom}] bouton_apres_defilement=[${apres.top},${apres.bottom}] " +
            "accessible=${apres.visible}\n"
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt").appendText(line)
    }

    @Test
    fun k01_ajout_bouton_enregistrer_atteignable_clavier_ouvert() {
        startMain()
        onView(withId(R.id.menu_add)).perform(click())
        onView(allOf(withId(R.id.inputStore), isDisplayed())).perform(click(), replaceText(store))
        E2e.closeKeyboard() // clavier du champ Magasin fermé : sinon, à police 2,0, le champ montant reste masqué
        onView(withId(R.id.inputAmount)).perform(scrollTo(), click())
        // Taper seulement quand le clavier est prêt : sinon l'injection perd le 1er caractère (« ,20 » observé, run 36631237084)
        assertTrue("Clavier non affiché", waitImeStable() > 0)
        onView(withId(R.id.inputAmount)).perform(typeText("4,20"))
        onView(withId(R.id.inputAmount)).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("4,20")))
        val avant = mesure(R.id.btnSaveTicket)
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo())
        waitImeStable()
        val apres = mesure(R.id.btnSaveTicket)
        log("Ajouter", avant, apres)
        shot("k01_ajout_clavier_${passe}")
        assertTrue("Bouton Enregistrer masqué par le clavier après défilement : $apres", apres.visible)
        var saisi = ""
        instr.runOnMainSync { saisi = resumedOnMain().findViewById<android.widget.TextView>(R.id.inputAmount).text.toString() }
        onView(withId(R.id.btnSaveTicket)).perform(click()) // vrai toucher à l'emplacement mesuré
        // Enregistrement asynchrone (coroutine) : on attend jusqu'à 5 s
        var saved = false
        val end = SystemClock.uptimeMillis() + 5000
        while (!saved && SystemClock.uptimeMillis() < end) {
            saved = runBlocking {
                AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first().any { it.store == store && it.amount == 4.20 }
            }
            if (!saved) SystemClock.sleep(200)
        }
        if (!saved) {
            shot("k01_apres_toucher_${passe}")
            E2e.diagnostic("k01 passe $passe : non enregistré")
        }
        val proches = runBlocking {
            AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first().filter { it.store.startsWith("Clavier") }
                .joinToString { "${it.store}=${it.amount}" }
        }
        assertTrue("Ticket non enregistré 5 s après le toucher du bouton (montant saisi « $saisi », tickets : $proches)", saved)
    }

    @Test
    fun k02_modification_bouton_enregistrer_atteignable_clavier_ouvert() {
        // Ticket propre à ce test (indépendant de k01)
        runBlocking {
            AppDatabase.getInstance(ctx).ticketDao().insert(com.etix.model.Ticket(store = "$store modif", amount = 2.0,
                category = "Autre", dateMillis = System.currentTimeMillis()))
        }
        startMain()
        onView(withId(R.id.menu_history)).perform(click())
        onView(withId(R.id.inputSearch)).perform(replaceText("$store modif"))
        E2e.closeKeyboard()
        // Le champ de recherche contient aussi le texte : on vise la ligne de la liste
        val row = allOf(withText("$store modif"), isDescendantOfA(withId(R.id.recyclerHistory)))
        waitFor(row)
        onView(allOf(row, isDisplayed())).perform(click())
        // Lot 6 : « Modifier » est sous la carte montant / date → défilement avant le toucher
        waitFor(allOf(withId(R.id.tvAmount), isDescendantOfA(withId(R.id.overlayContainer))))
        onView(allOf(withId(R.id.btnEdit), isDescendantOfA(withId(R.id.overlayContainer)))).perform(E2e.nestedScrollTo(), click())
        val amount = allOf(withId(R.id.inputAmount), isDescendantOfA(withId(R.id.overlayContainer)))
        waitFor(amount)
        onView(amount).perform(scrollTo(), click(), clearText())
        assertTrue("Clavier non affiché", waitImeStable() > 0)
        onView(amount).perform(typeText("5,30"))
        onView(amount).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("5,30")))
        val save = allOf(withId(R.id.btnSave), isDescendantOfA(withId(R.id.overlayContainer)))
        val avant = mesure(R.id.btnSave)
        onView(save).perform(scrollTo())
        waitImeStable()
        val apres = mesure(R.id.btnSave)
        log("Modifier", avant, apres)
        shot("k02_modification_clavier_${passe}")
        assertTrue("Bouton Enregistrer masqué par le clavier après défilement : $apres", apres.visible)
        onView(save).perform(click())
        // Retour au détail, défilé là où l'on était (bas) : on remonte au montant avant de le lire
        val shownAmount = allOf(withId(R.id.tvAmount), isDescendantOfA(withId(R.id.overlayContainer)))
        SystemClock.sleep(1500) // fermeture de l'édition (animation)
        onView(shownAmount).perform(E2e.nestedScrollTo())
        waitFor(allOf(shownAmount, withText("5,30 €")))
        shot("k03_detail_apres_modification_${passe}")
    }

    /**
     * Lot 7 : saisie du budget, clavier ouvert — « Appliquer » (barre haute, comme iOS) mesuré par rapport au haut du
     * clavier, puis réellement touché ; le budget doit être enregistré.
     */
    @Test
    fun k04_budget_appliquer_atteignable_clavier_ouvert() {
        runBlocking {
            AppDatabase.getInstance(ctx).ticketDao().insert(com.etix.model.Ticket(store = "Clavier budget $passe",
                amount = 3.0, category = "Clavier $passe", dateMillis = System.currentTimeMillis()))
        }
        startMain()
        BudgetE2e.openCategoriesMonth()
        onView(withId(R.id.btnBudgets)).perform(click())
        waitFor(withId(R.id.tvBudgetsTitle))
        onView(allOf(withId(R.id.tvBudgetCategory), withText("Clavier $passe"))).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.inputBudget))
        assertTrue("Clavier non affiché", waitImeStable() > 0)
        onView(withId(R.id.inputBudget)).perform(typeText("7,50"))
        onView(withId(R.id.inputBudget)).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText("7,50")))
        waitImeStable()
        val m = mesure(R.id.btnBudgetApply)
        log("Budget", m, m)
        shot("k04_budget_clavier_${passe}")
        assertTrue("« Appliquer » masqué par le clavier : $m", m.visible)
        onView(withId(R.id.btnBudgetApply)).perform(click()) // vrai toucher
        SystemClock.sleep(500)
        val saved = com.etix.data.BudgetStore(ctx).limit("Clavier $passe")
        assertTrue("Budget non enregistré ($saved)", saved == 7.5)
    }

    /** Lot 8 : carte Budget de l'Accueil sur petit écran / grande police (budget de k04). */
    @Test
    fun k05_accueil_carte_budget() {
        startMain()
        AccueilBudgetE2e.checkCard("k05_accueil_budget_${passe}")
    }

    @Test
    fun k06_accueil_tendance() {
        startMain()
        AccueilBudgetE2e.checkTrend("k06_accueil_tendance_${passe}")
    }

    /**
     * Lot 9 : écran d'accueil du scanner sur petit écran / grande police. Mesuré sur l'émulateur (pas un rendu
     * simulé) : barre basse ≤ 40 % de la hauteur utile, barre d'onglets masquée, les trois boutons entièrement
     * visibles sans défilement. Mesures dans shots/mesures_scan.txt (publiées par la CI).
     */
    @Test
    fun k07_scan_intro() {
        startMain()
        onView(withId(R.id.menu_add)).perform(click())
        waitFor(withId(R.id.inputStore))
        onView(allOf(withId(R.id.btnScanTicket), isDisplayed())).perform(click())
        waitFor(withId(R.id.btnTakePhoto))
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        shot("k07_scan_intro_${passe}")
        var bar = 0; var step = 0; var screen = 0; var compact = false; var nav = View.VISIBLE
        var a: Activity? = null
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val act = resumedOnMain(); a = act
            bar = act.findViewById<View>(R.id.introBar).height
            step = act.findViewById<View>(R.id.stepIntro).height
            screen = act.window.decorView.height
            compact = act.findViewById<View>(R.id.introCompactActions).visibility == View.VISIBLE
            nav = act.findViewById<View>(R.id.bottomNav).visibility
        }
        val res = a!!.resources
        java.io.File(java.io.File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_scan.txt").appendText(
            "passe=$passe sdk=${Build.VERSION.SDK_INT} écran=${res.configuration.screenWidthDp}x${res.configuration.screenHeightDp} dp " +
                "police=${res.configuration.fontScale} : barre basse $bar px / étape $step px " +
                "(${if (step > 0) bar * 100 / step else -1} %), fenêtre $screen px, compact=$compact, " +
                "barre d'onglets ${if (nav == View.VISIBLE) "visible" else "masquée"}\n")
        assertTrue("barre d'onglets masquée pendant le scan", nav != View.VISIBLE)
        assertTrue("barre basse $bar px > 40 % de $step px", step > 0 && bar <= step * com.etix.ui.scan.ScanFlowFragment.MAX_BAR_FRACTION)
        // actions toujours atteignables : les trois boutons entièrement à l'écran, sans défilement
        for (id in listOf(R.id.btnTakePhoto, R.id.btnPickImage, R.id.btnScanCancel)) {
            onView(withId(id)).check(androidx.test.espresso.assertion.ViewAssertions.matches(
                androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed()))
        }
        onView(withId(R.id.btnScanCancel)).perform(click())
        waitFor(withId(R.id.inputStore))
    }

    /** Lot 9 : formulaire prérempli par un scan (ML Kit réel, image simulée) sur petit écran / grande police. */
    @Test
    fun k08_scan_formulaire_prerempli() {
        androidx.test.espresso.intent.Intents.init()
        try {
            val before = ScanE2e.tickets()
            startMain()
            onView(withId(R.id.menu_add)).perform(click())
            waitFor(withId(R.id.inputStore))
            onView(allOf(withId(R.id.btnScanTicket), isDisplayed())).perform(click())
            waitFor(withId(R.id.btnTakePhoto))
            ScanE2e.stubPicker(ScanE2e.ticketImage("petit_${passe}.png"))
            onView(withId(R.id.btnPickImage)).perform(click())
            waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)
            shot("k08_scan_formulaire_${passe}")
            onView(withId(R.id.inputStore)).check(androidx.test.espresso.assertion.ViewAssertions.matches(
                androidx.test.espresso.matcher.ViewMatchers.withText("ESSO")))
            onView(withId(R.id.btnDiscardScan)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
            org.junit.Assert.assertEquals(before, ScanE2e.tickets())
        } finally {
            androidx.test.espresso.intent.Intents.release()
        }
    }

    /**
     * Lot 10 : Réglages sur petit écran / grande police. Lignes Thème et Période : libellé et valeur entiers (ni
     * tronqués ni coupés en milieu de mot) ; liste de choix lisible ; bas de l'écran atteignable par défilement.
     * Passe d (320 dp, police 2,0) : aussi en thème sombre, puis Système rétabli. Mesures : shots/mesures_reglages.txt.
     */
    @Test
    fun k09_reglages() {
        startMain()
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.rowTheme))
        val problems = mutableListOf<String>()
        val measures = StringBuilder()
        instr.runOnMainSync {
            val act = resumedOnMain()
            val res = act.resources
            measures.append("passe=$passe sdk=${Build.VERSION.SDK_INT} écran=${res.configuration.screenWidthDp}dp " +
                "police=${res.configuration.fontScale} :")
            for (row in listOf(R.id.rowTheme, R.id.rowDefaultRange)) {
                val g = act.findViewById<android.view.ViewGroup>(row)
                val inner = (0 until g.childCount).map { g.getChildAt(it) }
                    .flatMap { v -> if (v is android.view.ViewGroup) (0 until v.childCount).map { v.getChildAt(it) } else listOf(v) }
                for (child in inner) {
                    val t = child as? android.widget.TextView ?: continue
                    val l = t.layout ?: continue
                    val ell = (0 until l.lineCount).sumOf { l.getEllipsisCount(it) }
                    val words = t.text.toString().split(' ', '\u00A0').size
                    measures.append(" « ${t.text} » ${l.lineCount} ligne(s)${if (ell > 0) " tronqué" else ""}")
                    if (ell > 0 || l.lineCount > words) problems += "« ${t.text} » : ${l.lineCount} lignes, $ell caractères masqués"
                }
            }
        }
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_reglages.txt").appendText("$measures\n")
        shot("k09_reglages_haut_${passe}")
        onView(withId(R.id.rowDefaultRange)).perform(click())
        onView(withText("Cette année")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
            .check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()))
        shot("k09_reglages_choix_periode_${passe}")
        onView(withText("Annuler")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
        onView(withId(R.id.btnClearCrash)).perform(scrollTo())
            .check(androidx.test.espresso.assertion.ViewAssertions.matches(
                androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed()))
        shot("k09_reglages_bas_${passe}")
        if (passe == "d") {
            onView(withId(R.id.rowTheme)).perform(scrollTo())
            E2e.chooseTheme("Sombre")
            waitFor(allOf(withId(R.id.tvThemeValue), withText("Sombre")))
            shot("k09_reglages_sombre_haut_${passe}")
            onView(withId(R.id.btnClearCrash)).perform(scrollTo())
            shot("k09_reglages_sombre_bas_${passe}")
            onView(withId(R.id.rowTheme)).perform(scrollTo())
            E2e.chooseTheme("Système")
            waitFor(allOf(withId(R.id.tvThemeValue), withText("Système")))
        }
        assertTrue("Réglages petit écran : ${problems.joinToString("; ")}", problems.isEmpty())
    }
}
