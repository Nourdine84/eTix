package com.etix.e2e

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isDialog
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
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.Locale

/**
 * Revue visuelle de l'Historique (captures seulement, aucun changement de l'app) : mêmes données FICTIVES que la
 * revue de l'Accueil (créées par E2eRevueAccueilTest, lancé avant dans le mode « revue »), émulateur en français.
 * Clair puis Sombre : liste (haut et bas), recherche, fenêtre « Filtres » (date de début proposée par défaut),
 * filtre appliqué, export des tickets affichés (feuille de partage interceptée, contenu lu). Recherche et filtre remis
 * à zéro ensuite ; aucune donnée modifiée. Argument facultatif `passe` (ex. « petit » : 320 dp, police 2,0) ajouté au
 * nom des captures. Mesures des cartes (nom de magasin, montant) consignées dans shots/mesures_clavier.txt.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eRevueHistoriqueTest {

    private fun allTickets() = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eRevueHistoriqueTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun onActivity(block: (android.app.Activity) -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            block(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first())
        }

    private fun scrollList(toEnd: Boolean) {
        onActivity { a ->
            val r = a.findViewById<RecyclerView>(R.id.recyclerHistory)
            r.scrollToPosition(if (toEnd) (r.adapter?.itemCount ?: 1) - 1 else 0)
        }
        Thread.sleep(600)
    }

    private fun shown(id: Int): Boolean {
        var v = false
        onActivity { a -> v = a.findViewById<View>(id).isShown }
        return v
    }

    private fun chooseTheme(theme: String) {
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.rowTheme))
        E2e.chooseTheme(theme)
        waitFor(withId(R.id.rowTheme))
        pressBack()
        E2e.waitForAppReady("E2eRevueHistoriqueTest.$theme"); waitFor(withId(R.id.bottomNav))
    }

    private val passe: String? = InstrumentationRegistry.getArguments().getString("passe")

    /**
     * Cartes ticket affichées : nombre de lignes du nom, nom tronqué (…), mot coupé en fin de ligne, montant
     * entièrement visible et sans chevauchement avec le nom. Retourne les anomalies ; mesures consignées.
     */
    private fun measureCards(label: String): List<String> {
        val problems = mutableListOf<String>()
        val out = StringBuilder("historique $label sdk=${android.os.Build.VERSION.SDK_INT}")
        onActivity { a ->
            val res = a.resources
            out.append(" largeur=${res.configuration.screenWidthDp}dp police=${res.configuration.fontScale} :")
            val r = a.findViewById<RecyclerView>(R.id.recyclerHistory)
            for (i in 0 until r.childCount) {
                val card = r.getChildAt(i)
                val name = card.findViewById<android.widget.TextView>(R.id.tvStore) ?: continue
                val amount = card.findViewById<android.widget.TextView>(R.id.tvAmount)
                val l = name.layout ?: continue
                val text = name.text.toString()
                val lines = l.lineCount
                val ellipsized = l.getEllipsisCount(lines - 1) > 0
                val cut = (0 until lines - 1).any { li ->
                    val end = l.getLineEnd(li)
                    end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()
                }
                val ra = android.graphics.Rect(); val rn = android.graphics.Rect(); val rc = android.graphics.Rect()
                val amountVisible = amount.getGlobalVisibleRect(ra) && ra.width() == amount.width && ra.height() == amount.height
                name.getGlobalVisibleRect(rn); card.getGlobalVisibleRect(rc)
                val overlap = android.graphics.Rect.intersects(ra, rn)
                val fullyOnScreen = rc.height() == card.height
                out.append(" [$text : ${lines} ligne(s)${if (ellipsized) ", tronqué" else ""}${if (cut) ", mot coupé" else ""}" +
                    ", montant ${amount.text}${if (amountVisible) "" else " masqué"}${if (overlap) ", chevauchement" else ""}]")
                if (!fullyOnScreen) continue // carte en bord d'écran : pas de verdict sur sa visibilité
                if (lines > 2) problems += "$text : $lines lignes"
                if (ellipsized) problems += "$text : tronqué"
                if (cut) problems += "$text : mot coupé"
                if (!amountVisible) problems += "$text : montant masqué"
                if (overlap) problems += "$text : montant sur le nom"
            }
        }
        java.io.File(java.io.File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt")
            .appendText(out.toString() + "\n")
        return problems
    }

    /** Export des tickets affichés : feuille de partage interceptée, nombre de lignes du CSV = nombre affiché. */
    private fun checkExport(label: String) {
        var text = ""
        onActivity { a -> text = a.findViewById<android.widget.Button>(R.id.btnExportCsv).text.toString() }
        val n = Regex("""Exporter les (\d+) tickets affichés""").find(text)!!.groupValues[1].toInt()
        androidx.test.espresso.intent.Intents.init()
        try {
            androidx.test.espresso.intent.Intents.intending(
                androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(Intent.ACTION_CHOOSER))
                .respondWith(android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null))
            onView(allOf(withId(R.id.btnExportCsv), androidx.test.espresso.matcher.ViewMatchers.isDisplayed())).perform(click())
            val (_, bytes) = ReglagesE2e.sharedContent(ReglagesE2e.lastChooser())
            val lines = String(bytes, Charsets.UTF_8).trimEnd('\n').split('\n')
            assertEquals("CSV exporté ($label) : en-tête + $n tickets", n + 1, lines.size)
        } finally {
            androidx.test.espresso.intent.Intents.release()
        }
    }

    /** Part visible (%) d'un bouton de la fenêtre « Filtres » (0 si absent ou hors écran). */
    private fun dialogButtonPct(label: String): Int {
        var pct = 0
        try {
            onView(withText(label)).inRoot(isDialog()).check { v, _ ->
                val r = android.graphics.Rect()
                if (v != null && v.width * v.height > 0 && v.getGlobalVisibleRect(r))
                    pct = 100 * r.width() * r.height() / (v.width * v.height)
            }
        } catch (_: Throwable) { }
        return pct
    }

    /**
     * Glissement vers le haut dans la partie VISIBLE d'une zone défilante (swipeUp d'Espresso exige que la vue soit
     * visible à 90 %, ce qui n'est pas garanti pour une zone en partie hors écran).
     */
    private fun swipeUpInVisiblePart() = object : androidx.test.espresso.ViewAction {
        override fun getConstraints(): org.hamcrest.Matcher<View> =
            androidx.test.espresso.matcher.ViewMatchers.isDisplayed()
        override fun getDescription() = "glissement vers le haut dans la partie visible"
        override fun perform(uiController: androidx.test.espresso.UiController, view: View) {
            val r = android.graphics.Rect()
            view.getGlobalVisibleRect(r)
            val off = IntArray(2); view.rootView.getLocationOnScreen(off)
            // Coordonnées écran : rectangle visible (fenêtre) décalé de la position de la fenêtre
            val x = (off[0] + r.centerX()).toFloat()
            val y0 = (off[1] + r.top + r.height() * 0.85f)
            val y1 = (off[1] + r.top + r.height() * 0.15f)
            androidx.test.espresso.action.Swipe.SLOW.sendSwipe(uiController, floatArrayOf(x, y0), floatArrayOf(x, y1),
                floatArrayOf(1f, 1f))
            uiController.loopMainThreadForAtLeast(300)
        }
    }

    /**
     * Parcours utilisateur vers un bouton de la fenêtre « Filtres » : s'il n'est pas entièrement visible, glissement
     * vers le haut dans la zone défilante qui le contient (comme le ferait l'utilisateur), puis vérification BLOQUANTE
     * qu'il est visible à 90 % au moins (seuil d'Espresso pour un appui) avant de l'utiliser.
     */
    private fun pressDialogButton(label: String, suffix: String) {
        val before = dialogButtonPct(label)
        var after = before
        var swipes = 0
        while (after < 90 && swipes < 3) {
            onView(allOf(
                org.hamcrest.Matchers.anyOf(
                    androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(android.widget.ScrollView::class.java),
                    androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(androidx.core.widget.NestedScrollView::class.java)),
                androidx.test.espresso.matcher.ViewMatchers.hasDescendant(withText(label))))
                .inRoot(isDialog()).perform(swipeUpInVisiblePart())
            Thread.sleep(400)
            after = dialogButtonPct(label); swipes++
        }
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt").appendText(
            "historique fenêtre Filtres $suffix : « $label » visible $before %" +
                (if (swipes > 0) ", après $swipes glissement(s) : $after %" else "") + "\n")
        assertTrue("« $label » accessible dans la fenêtre Filtres ($suffix) : $after % visible", after >= 90)
        if (swipes > 0 && label == "Réinitialiser") shot("revue_historique_4b_filtres_defilement_$suffix")
        onView(withText(label)).inRoot(isDialog()).perform(click())
    }

    /** Nombre de tickets affichés, lu sur le bouton d'export (liste, recherche et filtre appliqués). */
    private fun waitCount(n: Int) =
        waitFor(allOf(withId(R.id.btnExportCsv), withText("Exporter les $n tickets affichés (CSV)")))

    private fun captureHistorique(theme: String, themeSuffix: String) {
        val suffix = if (passe == null) themeSuffix else "${themeSuffix}_$passe"
        startMain()
        chooseTheme(theme)
        onView(withId(R.id.menu_history)).perform(click())
        waitFor(withId(R.id.recyclerHistory))
        waitCount(11)

        val problems = mutableListOf<String>()
        scrollList(toEnd = false)
        problems += measureCards("liste haut $suffix")
        shot("revue_historique_1_liste_haut_$suffix")
        scrollList(toEnd = true)
        problems += measureCards("liste bas $suffix")
        shot("revue_historique_2_liste_bas_$suffix")
        scrollList(toEnd = false)

        // Recherche (saisie directe, clavier fermé pour voir les résultats)
        onView(withId(R.id.inputSearch)).perform(replaceText("Brasserie"))
        E2e.closeKeyboard()
        waitCount(2) // deux tickets « Brasserie de la Gare » (ce mois et le mois dernier)
        onView(allOf(withText("Marché du Centre"), androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
            .check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
        problems += measureCards("recherche $suffix")
        shot("revue_historique_3_recherche_$suffix")
        onView(withId(R.id.inputSearch)).perform(replaceText(""))
        E2e.closeKeyboard()
        waitCount(11)

        // Filtres : date de début activée (valeur proposée par défaut : il y a un mois)
        onView(withId(R.id.btnFilter)).perform(click())
        onView(withId(R.id.switchStart)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.tvStart)).inRoot(isDialog())
            .check(androidx.test.espresso.assertion.ViewAssertions.matches(
                androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
        shot("revue_historique_4_filtres_$suffix")
        pressDialogButton("Appliquer", suffix)
        Thread.sleep(600)
        assertTrue("Résumé du filtre affiché ($suffix)", shown(R.id.tvFilterSummary))
        // Résumé et bouton d'export sur deux lignes distinctes, sans chevauchement
        var summaryBottom = 0; var exportTop = 0
        onActivity { a ->
            val rs = android.graphics.Rect(); val re = android.graphics.Rect()
            a.findViewById<View>(R.id.tvFilterSummary).getGlobalVisibleRect(rs)
            a.findViewById<View>(R.id.btnExportCsv).getGlobalVisibleRect(re)
            summaryBottom = rs.bottom; exportTop = re.top
        }
        assertTrue("Résumé du filtre au-dessus du bouton d'export ($suffix) : $summaryBottom ≤ $exportTop",
            summaryBottom <= exportTop)
        scrollList(toEnd = false)
        problems += measureCards("filtre actif $suffix")
        shot("revue_historique_5_filtre_actif_$suffix")
        checkExport("filtre actif $suffix")

        // « Fermer » : fenêtre fermée, filtre inchangé (même nombre de tickets affichés, résumé toujours visible)
        var exportLabel = ""
        onActivity { a -> exportLabel = a.findViewById<android.widget.Button>(R.id.btnExportCsv).text.toString() }
        onView(withId(R.id.btnFilter)).perform(click())
        pressDialogButton("Fermer", suffix)
        waitFor(allOf(withId(R.id.btnExportCsv), withText(exportLabel)))
        assertTrue("Filtre conservé après « Fermer » ($suffix)", shown(R.id.tvFilterSummary))

        // « Réinitialiser » : filtre retiré, tous les tickets de nouveau affichés
        onView(withId(R.id.btnFilter)).perform(click())
        pressDialogButton("Réinitialiser", suffix)
        waitCount(11)
        assertFalse("Résumé du filtre masqué ($suffix)", shown(R.id.tvFilterSummary))
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt")
            .appendText("historique $suffix anomalies : ${if (problems.isEmpty()) "aucune" else problems.distinct().joinToString(" ; ")}\n")
        assertEquals("Cartes de l'Historique ($suffix) : nom sur 2 lignes au plus, sans mot coupé ni troncature, " +
            "montant visible", emptyList<String>(), problems.distinct())
    }

    @Test fun b01_historique_clair_puis_sombre() {
        assertEquals("Émulateur attendu en français", "fr", Locale.getDefault().language)
        val before = allTickets(); val budgetsBefore = BudgetStore(ctx).load()
        assertEquals("Données de la revue de l'Accueil attendues (11 tickets fictifs)", 11, before.size)
        captureHistorique("Clair", "clair")
        captureHistorique("Sombre", "sombre")
        chooseTheme("Système")
        assertEquals(before, allTickets()); assertEquals(budgetsBefore, BudgetStore(ctx).load())
    }
}
