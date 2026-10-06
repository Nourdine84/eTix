package com.etix.e2e

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
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
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.features.ticket.DatePickerRules
import com.etix.ui.main.MainActivityV2
import com.etix.ui.ticket.DatePickerPresentation
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * Sélecteur de date autour du seuil de bascule calendrier / saisie (DatePickerRules), à 320 dp : passes « seuil10 »,
 * « seuil115 », « seuil15 » et « seuil20 » (police 1,0, 1,15, 1,5 et 2,0, tailles proposées par Android 14 ; seuil
 * attendu entre 1,0 et 1,15, fixé par le libellé du mois le plus long). Thème Système (clair sur l'émulateur), écran Ajouter, mêmes données fictives (E2eRevueAccueilTest), rien
 * d'enregistré. BLOQUANT :
 * - présentation à l'ouverture conforme à la règle, calendrier lisible (jours, jours de la semaine, mois, y compris le
 *   mois le plus long « Septembre ») ou format indiqué en saisie ;
 * - en saisie : 31/02/2026 → « Date invalide », 2026/02/31 → « Format incorrect », OK inactif ; actions, champ et
 *   message entièrement visibles entre la barre d'état et le clavier ;
 * - bascule manuelle vers le calendrier puis retour : l'icône de bascule reste entièrement visible et ramène à la
 *   saisie (la lisibilité du calendrier choisi manuellement est mesurée et consignée, non bloquante) ;
 * - annulation sans effet sur la date, aucune donnée modifiée.
 */
@RunWith(AndroidJUnit4::class)
class E2eRevueDateSeuilTest {

    private val instr get() = InstrumentationRegistry.getInstrumentation()
    private val passe: String = InstrumentationRegistry.getArguments().getString("passe") ?: "sans_passe"
    private val toggle = com.google.android.material.R.id.mtrl_picker_header_toggle

    private fun resumed() = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first()

    private fun allTickets() = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }

    private fun log(line: String) {
        val c = ctx.resources.configuration
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt").appendText(
            "seuil $passe $line sdk=${Build.VERSION.SDK_INT} largeur=${c.screenWidthDp}dp police=${c.fontScale}\n")
    }

    private val textInputField = allOf(isAssignableFrom(EditText::class.java),
        isDescendantOfA(withId(com.google.android.material.R.id.mtrl_picker_text_input_date)))

    private fun inTextMode(): Boolean = try {
        onView(textInputField).check(matches(isDisplayed())); true
    } catch (_: Throwable) { false }

    private fun pressToggle() {
        onView(withId(toggle)).perform(click())
        SystemClock.sleep(600)
    }

    /** Message d'erreur du champ de date, attendu au plus 3 s (Material le poste après la saisie). */
    private fun typeAndReadError(value: String): String {
        onView(textInputField).perform(click(), replaceText(value))
        var err: CharSequence? = null
        val end = SystemClock.uptimeMillis() + 3000
        while (err == null && SystemClock.uptimeMillis() < end) {
            SystemClock.sleep(200)
            onView(withId(com.google.android.material.R.id.mtrl_picker_text_input_date)).check { v, _ ->
                err = (v as TextInputLayout).error
            }
        }
        onView(withId(com.google.android.material.R.id.confirm_button)).check { v, _ ->
            assertTrue("OK inactif pour « $value » ($passe)", !v.isEnabled)
        }
        return err?.toString().orEmpty()
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

    /** Libellés coupés du calendrier affiché (jours, jours de la semaine, mois). */
    private fun calendarProblems(): List<String> {
        val problems = mutableListOf<String>()
        for (id in listOf(com.google.android.material.R.id.mtrl_calendar_days_of_week,
                com.google.android.material.R.id.mtrl_calendar_months,
                com.google.android.material.R.id.month_navigation_fragment_toggle)) {
            try { onView(allOf(withId(id), isDisplayed())).check { v, _ -> collectClipped(v, problems) } } catch (_: Throwable) {}
        }
        return problems.distinct()
    }

    /** Libellé du mois affiché dans l'en-tête du calendrier, avec sa largeur réelle et la place disponible (dp). */
    private fun monthLabel(): String {
        var label = ""
        onView(withId(com.google.android.material.R.id.month_navigation_fragment_toggle)).check { v, _ ->
            val t = v as android.widget.TextView
            val d = t.resources.displayMetrics.density
            // Place maximale : largeur du conteneur du bouton (le bouton s'ajuste au texte), marges et icône retirées
            val max = (t.parent as View).width - t.totalPaddingLeft - t.totalPaddingRight
            label = "${t.text} (${"%.1f".format(t.paint.measureText(t.text.toString()) / d)}dp / " +
                "${"%.1f".format(max / d)}dp au plus)"
        }
        return label
    }

    /** Zone utile de l'écran (bas de la barre d'état, haut du clavier ou bas de l'écran), mesurée sur l'activité. */
    private fun usableZone(): Pair<Int, Int> {
        var z = 0 to 0
        instr.runOnMainSync {
            val decor = resumed().window.decorView
            val ins = ViewCompat.getRootWindowInsets(decor)
            val d = IntArray(2); decor.getLocationOnScreen(d)
            val bars = ins?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            val ime = ins?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            z = (d[1] + bars) to (d[1] + decor.height - ime)
        }
        return z
    }

    private fun assertWithin(id: Int, what: String, label: String) {
        SystemClock.sleep(700) // clavier et défilement stabilisés
        val (top, bottom) = usableZone()
        onView(withId(id)).check { v, _ ->
            val loc = IntArray(2); v.getLocationOnScreen(loc)
            val r = android.graphics.Rect()
            val ok = v.getGlobalVisibleRect(r) && r.height() == v.height && r.width() == v.width &&
                loc[1] >= top && loc[1] + v.height <= bottom
            val line = "$label : $what [${loc[1]},${loc[1] + v.height}] zone [$top,$bottom] visible=$ok"
            log(line)
            assertTrue("Élément du sélecteur accessible ($line)", ok)
        }
    }

    private fun checkTextEntry(label: String) {
        val invalid = typeAndReadError("31/02/2026")
        log("$label 31/02/2026 : « ${invalid.replace('\n', ' ')} »")
        assertTrue("31/02/2026 : « Date invalide » attendu ($label) : « $invalid »", invalid.startsWith("Date invalide"))
        for (id in listOf(com.google.android.material.R.id.confirm_button, com.google.android.material.R.id.cancel_button,
                com.google.android.material.R.id.mtrl_picker_text_input_date))
            assertWithin(id, ctx.resources.getResourceEntryName(id), "$label, date invalide")
        shot("revue_date_seuil_2_date_invalide_$passe")
        val badFormat = typeAndReadError("2026/02/31")
        log("$label 2026/02/31 : « ${badFormat.replace('\n', ' ')} »")
        assertTrue("2026/02/31 : « Format incorrect » attendu ($label) : « $badFormat »", badFormat.startsWith("Format incorrect"))
        assertWithin(com.google.android.material.R.id.mtrl_picker_text_input_date, "champ et message", "$label, format incorrect")
        shot("revue_date_seuil_3_format_incorrect_$passe")
    }

    @Test fun d01_bascule_calendrier_saisie_autour_du_seuil() {
        assertEquals("Émulateur attendu en français", "fr", Locale.getDefault().language)
        assertTrue("Passe seuil… attendue", passe.startsWith("seuil"))
        val before = allTickets(); val budgetsBefore = BudgetStore(ctx).load()
        assertEquals("Données de la revue de l'Accueil attendues (11 tickets fictifs)", 11, before.size)
        val conf = ctx.resources.configuration
        assertEquals("Largeur de la passe", 320, conf.screenWidthDp)
        val monthDp = DatePickerPresentation.longestMonthLabelDp(ctx)
        val expectText = DatePickerPresentation.prefersTextInput(ctx)
        log("règle : colonne ${"%.1f".format(DatePickerRules.calendarColumnDp(conf.screenWidthDp))}dp, " +
            "jour à 2 chiffres ${"%.1f".format(DatePickerRules.twoDigitDayDp(conf.fontScale))}dp, mois le plus long " +
            "${"%.1f".format(monthDp)}dp + marge ${DatePickerRules.MONTH_LABEL_MARGIN_DP}dp / ${DatePickerRules.MONTH_LABEL_AVAILABLE_DP}dp → " +
            if (expectText) "saisie" else "calendrier")

        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eRevueDateSeuilTest"); waitFor(withId(R.id.bottomNav))
        onView(withId(R.id.menu_add)).perform(click())
        waitFor(allOf(withId(R.id.titleAdd), isDescendantOfA(withId(R.id.containerAdd))))
        var initial = ""
        onView(withId(R.id.inputDate)).check { v, _ -> initial = (v as android.widget.TextView).text.toString() }
        onView(withId(R.id.inputDate)).perform(scrollTo(), click())
        waitFor(withId(toggle))
        SystemClock.sleep(600)

        // 1. Présentation par défaut : conforme à la règle et lisible
        assertEquals("Présentation à l'ouverture ($passe)", expectText, inTextMode())
        if (expectText) {
            onView(withId(com.google.android.material.R.id.mtrl_picker_title_text)).check { v, _ ->
                val t = (v as android.widget.TextView).text.toString()
                assertTrue("Format indiqué dans le titre ($passe) : « $t »", t.contains("jj/mm/aaaa"))
            }
            log("ouverture : saisie (jj/mm/aaaa)")
        } else {
            val p = calendarProblems()
            log("ouverture : calendrier ${monthLabel()}, libellés coupés : ${if (p.isEmpty()) "aucun" else p.take(6).joinToString()}")
            assertEquals("Calendrier lisible ($passe)", emptyList<String>(), p.take(10))
        }
        shot("revue_date_seuil_1_ouverture_$passe")
        if (!expectText) {
            // Mois le plus long en français : « Septembre » (navigation par les flèches, sans rien choisir)
            for (i in 0 until 12) {
                if (monthLabel().startsWith("Septembre")) break
                onView(withId(com.google.android.material.R.id.month_navigation_previous)).perform(click())
                SystemClock.sleep(400)
            }
            assertTrue("Mois de septembre affiché ($passe)", monthLabel().startsWith("Septembre"))
            val p = calendarProblems()
            log("calendrier ${monthLabel()} (mois le plus long), libellés coupés : ${if (p.isEmpty()) "aucun" else p.take(6).joinToString()}")
            assertEquals("Calendrier lisible, mois le plus long ($passe)", emptyList<String>(), p.take(10))
            shot("revue_date_seuil_1b_mois_le_plus_long_$passe")
        }

        // 2. Saisie : messages distincts, actions et message accessibles clavier ouvert
        if (!expectText) pressToggle()
        assertTrue("Saisie affichée ($passe)", inTextMode())
        checkTextEntry(if (expectText) "saisie par défaut" else "saisie choisie depuis le calendrier")

        // 3. Bascule manuelle vers le calendrier, puis retour à la saisie toujours accessible
        pressToggle()
        assertTrue("Calendrier affiché après bascule ($passe)", !inTextMode())
        val p = calendarProblems()
        log("calendrier choisi manuellement : libellés coupés : ${if (p.isEmpty()) "aucun" else p.take(6).joinToString()}")
        assertWithin(toggle, "icône de retour à la saisie", "calendrier choisi manuellement")
        shot("revue_date_seuil_4_calendrier_manuel_$passe")
        pressToggle()
        assertTrue("Retour à la saisie ($passe)", inTextMode())
        shot("revue_date_seuil_5_retour_saisie_$passe")

        // 4. Annulation : date inchangée, rien d'enregistré
        onView(withId(com.google.android.material.R.id.cancel_button)).perform(click())
        waitFor(allOf(withId(R.id.inputDate), withText(initial)))
        E2e.closeKeyboard()
        assertEquals("Aucun ticket créé ni modifié", before, allTickets())
        assertEquals(budgetsBefore, BudgetStore(ctx).load())
    }
}
