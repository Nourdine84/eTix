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
import java.util.Locale

/**
 * Revue visuelle de l'Historique (captures seulement, aucun changement de l'app) : mêmes données FICTIVES que la
 * revue de l'Accueil (créées par E2eRevueAccueilTest, lancé avant dans le mode « revue »), émulateur en français.
 * Clair puis Sombre : liste (haut et bas), recherche, fenêtre « Filtres » (date de début proposée par défaut),
 * filtre appliqué. Recherche et filtre remis à zéro ensuite ; aucune donnée modifiée.
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

    private fun captureHistorique(theme: String, suffix: String) {
        startMain()
        chooseTheme(theme)
        onView(withId(R.id.menu_history)).perform(click())
        waitFor(withId(R.id.recyclerHistory))
        waitFor(withText("Réseau de bus"))

        scrollList(toEnd = false)
        shot("revue_historique_1_liste_haut_$suffix")
        scrollList(toEnd = true)
        shot("revue_historique_2_liste_bas_$suffix")
        scrollList(toEnd = false)

        // Recherche (saisie directe, clavier fermé pour voir les résultats)
        onView(withId(R.id.inputSearch)).perform(replaceText("Brasserie"))
        E2e.closeKeyboard()
        waitFor(withText("Brasserie de la Gare"))
        onView(allOf(withText("Marché du Centre"), androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
            .check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
        shot("revue_historique_3_recherche_$suffix")
        onView(withId(R.id.inputSearch)).perform(replaceText(""))
        E2e.closeKeyboard()
        waitFor(withText("Réseau de bus"))

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
        shot("revue_historique_5_filtre_actif_$suffix")

        // Filtre retiré
        onView(withId(R.id.btnFilter)).perform(click())
        onView(withText("Réinitialiser")).inRoot(isDialog()).perform(click())
        Thread.sleep(600)
        assertFalse("Résumé du filtre masqué ($suffix)", shown(R.id.tvFilterSummary))
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
