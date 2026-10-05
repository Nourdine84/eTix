package com.etix.e2e

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
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
import com.etix.data.BudgetStore
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.pickerText
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Revue visuelle de l'écran « Ajouter un ticket » (captures seulement, aucun changement de l'app) : émulateur en
 * français, mêmes données fictives que l'Accueil et l'Historique (créées par E2eRevueAccueilTest). Clair puis Sombre :
 * formulaire vide (haut, bas avec Enregistrer), sélecteur de date, choix de catégorie, formulaire rempli (haut, bas),
 * clavier ouvert sur le montant puis Enregistrer amené à l'écran. Le formulaire n'est PAS enregistré : aucune
 * donnée créée ni modifiée (vérifié). Argument facultatif `passe` (« petit » : 320 dp, police 2,0) ajouté aux noms.
 * Vérification bloquante : Enregistrer entièrement visible au-dessus du clavier après défilement.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eRevueAjouterTest {

    private val instr get() = InstrumentationRegistry.getInstrumentation()
    private val passe: String? = InstrumentationRegistry.getArguments().getString("passe")

    // Données fictives : ticket « Marché du Centre » de la revue de l'Accueil, daté du 1er du mois
    private val store = "Marché du Centre"
    private val amount = "42,80"
    private val category = "Alimentation"
    private val description = "Courses de la semaine"
    private val date: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

    private fun allTickets() = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }

    private fun resumed() = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first()

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eRevueAjouterTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun chooseTheme(theme: String) {
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.rowTheme))
        E2e.chooseTheme(theme)
        waitFor(withId(R.id.rowTheme))
        pressBack()
        E2e.waitForAppReady("E2eRevueAjouterTest.$theme"); waitFor(withId(R.id.bottomNav))
    }

    private fun scrollAdd(toEnd: Boolean) {
        instr.runOnMainSync {
            val s = resumed().findViewById<ScrollView>(R.id.scrollViewAdd)
            if (toEnd) s.fullScroll(View.FOCUS_DOWN) else s.scrollTo(0, 0)
        }
        SystemClock.sleep(500)
    }

    /** Hauteur du clavier en px, stable sur deux lectures ; 0 si absent après 8 s. */
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

    /** Enregistrer entièrement visible entre la barre d'état et le haut du clavier (coordonnées écran). */
    private fun saveAboveKeyboard(): String {
        var out = ""
        instr.runOnMainSync {
            val act = resumed()
            val decor = act.window.decorView
            val ins = ViewCompat.getRootWindowInsets(decor)
            val ime = ins?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val bars = ins?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            val d = IntArray(2); decor.getLocationOnScreen(d)
            val v = act.findViewById<View>(R.id.btnSaveTicket)
            val b = IntArray(2); v.getLocationOnScreen(b)
            val top = d[1] + bars; val bottom = d[1] + decor.height - ime
            val ok = ime > 0 && b[1] >= top && b[1] + v.height <= bottom
            out = "${if (ok) "OK" else "MASQUÉ"} bouton=[${b[1]},${b[1] + v.height}] zone=[$top,$bottom] clavier=${ime}px"
        }
        return out
    }

    private fun log(line: String) {
        val res = ctx.resources
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt").appendText(
            "ajouter $line sdk=${Build.VERSION.SDK_INT} largeur=${res.configuration.screenWidthDp}dp " +
                "police=${res.configuration.fontScale}\n")
    }

    private fun text(id: Int): String {
        var t = ""
        instr.runOnMainSync { t = resumed().findViewById<android.widget.TextView>(id).text.toString() }
        return t
    }

    private fun openDatePicker(dateFieldId: Int, inOverlay: Boolean = false) {
        val m = if (inOverlay) allOf(withId(dateFieldId), isDescendantOfA(withId(R.id.overlayContainer))) else withId(dateFieldId)
        onView(m).perform(scrollTo(), click())
        waitFor(withId(com.google.android.material.R.id.mtrl_picker_header_toggle))
    }

    private val textInputField = allOf(isAssignableFrom(EditText::class.java),
        isDescendantOfA(withId(com.google.android.material.R.id.mtrl_picker_text_input_date)))

    private fun pickerInTextMode(): Boolean = try {
        onView(textInputField).check(matches(isDisplayed())); true
    } catch (_: Throwable) { false }

    private fun ensureTextInput() {
        if (!pickerInTextMode()) onView(withId(com.google.android.material.R.id.mtrl_picker_header_toggle)).perform(click())
        onView(textInputField).check(matches(isDisplayed()))
    }

    private fun typeDate(value: String) {
        onView(textInputField).perform(click(), replaceText(value))
        SystemClock.sleep(300)
    }

    private fun assertConfirmEnabled(expected: Boolean, label: String) {
        onView(withId(com.google.android.material.R.id.confirm_button)).check { v, _ ->
            assertEquals("Bouton OK du sélecteur ${if (expected) "actif" else "inactif"} : $label", expected, v.isEnabled)
        }
    }

    /**
     * Sélecteur ouvert : en mode calendrier, chaque libellé (jours, jours de la semaine, mois) tient sur une ligne
     * sans être coupé ; en mode saisie, le titre indique le format jj/mm/aaaa. BLOQUANT. Retourne le mode constaté.
     */
    private fun checkPickerReadable(label: String): String {
        if (pickerInTextMode()) {
            onView(withId(com.google.android.material.R.id.mtrl_picker_title_text)).check { v, _ ->
                val t = (v as android.widget.TextView).text.toString()
                assertTrue("Format indiqué dans le titre de la saisie ($label) : « $t »", t.contains("jj/mm/aaaa"))
            }
            return "saisie (jj/mm/aaaa)"
        }
        val problems = mutableListOf<String>()
        onView(isAssignableFrom(android.widget.GridView::class.java).let { allOf(it, withId(com.google.android.material.R.id.mtrl_calendar_days_of_week)) })
            .check { v, _ -> collectClipped(v, problems) }
        onView(withId(com.google.android.material.R.id.mtrl_calendar_months)).check { v, _ -> collectClipped(v, problems) }
        onView(withId(com.google.android.material.R.id.month_navigation_fragment_toggle)).check { v, _ -> collectClipped(v, problems) }
        assertEquals("Calendrier lisible ($label)", emptyList<String>(), problems.distinct().take(10))
        return "calendrier"
    }

    private fun collectClipped(v: View?, out: MutableList<String>) {
        if (v == null || !v.isShown) return
        if (v is android.widget.TextView && v.text.isNotEmpty()) {
            val l = v.layout
            val avail = v.width - v.totalPaddingLeft - v.totalPaddingRight
            val w = v.paint.measureText(v.text.toString())
            if (l != null && (l.lineCount > 1 || l.getEllipsisCount(0) > 0 || w > avail + 1))
                out += "« ${v.text} » (${w.toInt()}/${avail}px, ${l.lineCount} ligne(s))"
        }
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) collectClipped(v.getChildAt(i), out)
    }

    /** Saisie au clavier : OK et Annuler entièrement visibles au-dessus du clavier. BLOQUANT. */
    private fun checkPickerActionsAboveKeyboard(label: String) {
        var ime = 0
        val end = SystemClock.uptimeMillis() + 8000
        while (ime == 0 && SystemClock.uptimeMillis() < end) {
            onView(withId(com.google.android.material.R.id.confirm_button)).check { v, _ ->
                ime = ViewCompat.getRootWindowInsets(v.rootView)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            if (ime == 0) SystemClock.sleep(250)
        }
        assertTrue("Clavier non affiché sur la saisie de date ($label)", ime > 0)
        SystemClock.sleep(500)
        for (id in listOf(com.google.android.material.R.id.confirm_button, com.google.android.material.R.id.cancel_button)) {
            onView(withId(id)).check { v, _ ->
                val loc = IntArray(2); v.getLocationOnScreen(loc)
                val root = v.rootView
                val rl = IntArray(2); root.getLocationOnScreen(rl)
                val ins = ViewCompat.getRootWindowInsets(root)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                val imeTop = rl[1] + root.height - ins // haut du clavier (coordonnées écran)
                val r = android.graphics.Rect()
                val visible = v.getGlobalVisibleRect(r) && r.height() == v.height && r.width() == v.width &&
                    loc[1] + v.height <= imeTop
                val line = "bouton ${v.resources.getResourceEntryName(id)} [${loc[1]},${loc[1] + v.height}] haut du clavier $imeTop (clavier ${ins}px) visible=$visible"
                log("$label sélecteur de date : $line")
                assertTrue("Action du sélecteur accessible clavier ouvert ($label) : $line", visible)
            }
        }
    }

    private fun checkOtherFields(label: String) {
        assertEquals("Magasin conservé ($label)", store, text(R.id.inputStore))
        assertEquals("Montant conservé ($label)", amount, text(R.id.inputAmount))
        assertEquals("Catégorie conservée ($label)", category, text(R.id.tvCategoryValue))
        assertEquals("Description conservée ($label)", description, text(R.id.inputDescription))
    }

    private fun captureAjouter(theme: String, themeSuffix: String) {
        val s = if (passe == null) themeSuffix else "${themeSuffix}_$passe"
        startMain()
        chooseTheme(theme)
        onView(withId(R.id.menu_add)).perform(click())
        waitFor(allOf(withId(R.id.titleAdd), isDescendantOfA(withId(R.id.containerAdd))))

        // 1-2 : formulaire vide
        scrollAdd(toEnd = false); shot("revue_ajouter_1_vide_haut_$s")
        scrollAdd(toEnd = true); shot("revue_ajouter_2_vide_bas_$s")
        scrollAdd(toEnd = false)

        // Remplissage (saisie directe : clavier fermé pour les captures du formulaire)
        onView(allOf(withId(R.id.inputStore), isDisplayed())).perform(replaceText(store))
        onView(withId(R.id.inputAmount)).perform(scrollTo(), replaceText(amount))
        onView(withId(R.id.rowCategory)).perform(scrollTo(), click())
        onData(hasToString(category)).inRoot(isDialog()).check(matches(isDisplayed()))
        shot("revue_ajouter_4_selecteur_categorie_$s")
        onData(hasToString(category)).inRoot(isDialog()).perform(click())
        waitFor(allOf(withId(R.id.tvCategoryValue), withText(category)))
        onView(withId(R.id.inputDescription)).perform(scrollTo(), replaceText(description))
        E2e.closeKeyboard()

        // 3 : sélecteur de date — présentation adaptée (calendrier lisible ou saisie jj/mm/aaaa), vérifiée
        val initialDate = text(R.id.inputDate)
        openDatePicker(R.id.inputDate)
        val mode = checkPickerReadable(s)
        shot("revue_ajouter_3_selecteur_date_$s")
        // Annulation : date et autres champs inchangés
        onView(withId(com.google.android.material.R.id.cancel_button)).perform(click())
        waitFor(allOf(withId(R.id.inputDate), withText(initialDate)))
        checkOtherFields("annulation $s")

        // Saisie : dates invalides refusées, 29/02 d'une année bissextile acceptée, actions accessibles clavier ouvert
        openDatePicker(R.id.inputDate)
        ensureTextInput()
        typeDate("31/02/2026"); assertConfirmEnabled(false, "31/02/2026 ($s)")
        shot("revue_ajouter_3b_date_invalide_$s")
        typeDate("29/02/2023"); assertConfirmEnabled(false, "29/02/2023, année non bissextile ($s)")
        typeDate("29/02/2024"); assertConfirmEnabled(true, "29/02/2024, année bissextile ($s)")
        checkPickerActionsAboveKeyboard(s)
        shot("revue_ajouter_3c_date_saisie_clavier_$s")
        onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
        val leap = Calendar.getInstance().apply { clear(); set(2024, Calendar.FEBRUARY, 29) }
        waitFor(allOf(withId(R.id.inputDate), withText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(leap.time))))
        checkOtherFields("validation $s")
        log("$s sélecteur de date : $mode ; annulation, dates invalides, 29/02/2024 et validation vérifiés")

        // Date des données fictives (1er du mois) pour les captures suivantes
        openDatePicker(R.id.inputDate)
        ensureTextInput()
        typeDate(pickerText(date))
        onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
        waitFor(allOf(withId(R.id.inputDate), withText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(date.time))))
        E2e.closeKeyboard()

        // 5-6 : formulaire rempli
        scrollAdd(toEnd = false); shot("revue_ajouter_5_rempli_haut_$s")
        scrollAdd(toEnd = true); shot("revue_ajouter_6_rempli_bas_$s")

        // 7-8 : clavier ouvert sur le montant, puis Enregistrer amené à l'écran (sans le toucher)
        scrollAdd(toEnd = false)
        onView(withId(R.id.inputAmount)).perform(scrollTo(), click())
        var ime = waitImeStable()
        if (ime == 0) { // un second toucher, comme le ferait l'utilisateur ; consigné
            log("$s clavier absent au 1er toucher, 2e toucher")
            onView(withId(R.id.inputAmount)).perform(click()); ime = waitImeStable()
        }
        assertTrue("Clavier non affiché ($s)", ime > 0)
        shot("revue_ajouter_7_clavier_montant_$s")
        val avant = saveAboveKeyboard()
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo())
        waitImeStable()
        val apres = saveAboveKeyboard()
        log("$s Enregistrer avant défilement : $avant ; après : $apres")
        shot("revue_ajouter_8_clavier_enregistrer_$s")
        assertTrue("Enregistrer accessible clavier ouvert ($s) : $apres", apres.startsWith("OK"))
        E2e.closeKeyboard()
    }

    /**
     * Modifier partage le sélecteur de date (TicketFormController) : même présentation adaptée, annulation sans effet,
     * aucune modification enregistrée (sortie par retour arrière).
     */
    @Test fun c02_modifier_meme_selecteur_de_date() {
        val before = allTickets()
        val s = if (passe == null) "modifier" else "modifier_$passe"
        startMain()
        onView(withId(R.id.menu_history)).perform(click())
        waitFor(allOf(withText("Réseau de bus"), isDescendantOfA(withId(R.id.recyclerHistory))))
        onView(allOf(withText("Réseau de bus"), isDescendantOfA(withId(R.id.recyclerHistory)), isDisplayed())).perform(click())
        val overlay = { id: Int -> allOf(withId(id), isDescendantOfA(withId(R.id.overlayContainer))) }
        waitFor(overlay(R.id.tvAmount))
        onView(overlay(R.id.btnEdit)).perform(E2e.nestedScrollTo(), click())
        waitFor(overlay(R.id.inputDate))
        var initial = ""
        onView(overlay(R.id.inputDate)).check { v, _ -> initial = (v as android.widget.TextView).text.toString() }
        openDatePicker(R.id.inputDate, inOverlay = true)
        val mode = checkPickerReadable(s)
        shot("revue_ajouter_9_modifier_selecteur_date_$s")
        onView(withId(com.google.android.material.R.id.cancel_button)).perform(click())
        waitFor(allOf(overlay(R.id.inputDate), withText(initial)))
        log("$s sélecteur de date (Modifier) : $mode ; annulation sans effet")
        pressBack() // quitte la modification sans enregistrer
        SystemClock.sleep(800)
        pressBack() // quitte le détail
        waitFor(withId(R.id.bottomNav))
        assertEquals("Aucun ticket modifié", before, allTickets())
    }

    @Test fun c01_ajouter_clair_puis_sombre() {
        assertEquals("Émulateur attendu en français", "fr", Locale.getDefault().language)
        assertTrue("Mesure du clavier (insets) : API 30+", Build.VERSION.SDK_INT >= 30)
        val before = allTickets(); val budgetsBefore = BudgetStore(ctx).load()
        assertEquals("Données de la revue de l'Accueil attendues (11 tickets fictifs)", 11, before.size)
        captureAjouter("Clair", "clair")
        captureAjouter("Sombre", "sombre")
        startMain()
        chooseTheme("Système")
        assertEquals("Aucun ticket créé ni modifié", before, allTickets())
        assertEquals(budgetsBefore, BudgetStore(ctx).load())
    }
}
