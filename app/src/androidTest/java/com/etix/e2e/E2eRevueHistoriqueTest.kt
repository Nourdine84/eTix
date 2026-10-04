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
        onView(withText("Appliquer")).inRoot(isDialog()).perform(click())
        Thread.sleep(600)
        assertTrue("Résumé du filtre affiché ($suffix)", shown(R.id.tvFilterSummary))
        scrollList(toEnd = false)
        problems += measureCards("filtre actif $suffix")
        shot("revue_historique_5_filtre_actif_$suffix")
        checkExport("filtre actif $suffix")

        // Filtre retiré
        onView(withId(R.id.btnFilter)).perform(click())
        onView(withText("Réinitialiser")).inRoot(isDialog()).perform(click())
        waitCount(11)
        assertFalse("Résumé du filtre masqué ($suffix)", shown(R.id.tvFilterSummary))
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt")
            .appendText("historique $suffix anomalies : ${if (problems.isEmpty()) "aucune" else problems.distinct().joinToString(" ; ")}\n")
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
