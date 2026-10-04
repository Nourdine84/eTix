package com.etix.e2e

import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
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
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.Calendar
import java.util.Locale

/**
 * Revue visuelle de l'Accueil (captures seulement, aucun changement de l'app) : émulateur en français, app neuve,
 * données FICTIVES injectées en base (aucun enregistrement par l'interface → aucun message temporaire), budgets
 * fictifs, période « Ce mois ». Mêmes données et même période en Clair puis en Sombre ; Accueil capturé par
 * défilement : haut, carte Budget, bas (Scanner un ticket, Ajout manuel, Voir l'historique).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eRevueAccueilTest {

    private fun at(monthsAgo: Int, day: Int): Long = Calendar.getInstance().apply {
        val today = get(Calendar.DAY_OF_MONTH)
        add(Calendar.MONTH, -monthsAgo)
        // Mois courant : jamais après aujourd'hui ; mois passés : jour borné à la longueur du mois
        val max = if (monthsAgo == 0) today else getActualMaximum(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, max))
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 30); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun tickets(): List<Ticket> = listOf(
        // Mois courant
        Ticket(store = "Marché du Centre", amount = 42.80, category = "Alimentation", dateMillis = at(0, 1)),
        Ticket(store = "Boulangerie des Halles", amount = 18.35, category = "Alimentation", dateMillis = at(0, 3)),
        Ticket(store = "Brasserie de la Gare", amount = 27.50, category = "Restaurant", dateMillis = at(0, 2)),
        Ticket(store = "Station du Pont", amount = 61.20, category = "Carburant", dateMillis = at(0, 4)),
        Ticket(store = "Réseau de bus", amount = 14.00, category = "Transport", dateMillis = at(0, 1)),
        // Cinq mois précédents (Tendance 6 mois, comparaison avec le mois dernier)
        Ticket(store = "Marché du Centre", amount = 95.40, category = "Alimentation", dateMillis = at(1, 10)),
        Ticket(store = "Brasserie de la Gare", amount = 32.00, category = "Restaurant", dateMillis = at(1, 18)),
        Ticket(store = "Marché du Centre", amount = 140.10, category = "Alimentation", dateMillis = at(2, 12)),
        Ticket(store = "Station du Pont", amount = 88.60, category = "Carburant", dateMillis = at(3, 9)),
        Ticket(store = "Marché du Centre", amount = 152.30, category = "Alimentation", dateMillis = at(4, 14)),
        Ticket(store = "Boulangerie des Halles", amount = 110.00, category = "Alimentation", dateMillis = at(5, 20)),
    )

    private val budgets = mapOf("Alimentation" to 150.0, "Restaurant" to 30.0, "Carburant" to 80.0)

    private fun allTickets() = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eRevueAccueilTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun onActivity(block: (android.app.Activity) -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            block(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first())
        }

    private fun scrollHome(target: (NestedScrollView, android.app.Activity) -> Int) {
        onActivity { a -> val s = a.findViewById<NestedScrollView>(R.id.homeScroll); s.scrollTo(0, target(s, a)) }
        Thread.sleep(600)
    }

    /** Vue entièrement visible à l'écran (pas seulement en partie). */
    private fun fullyVisible(id: Int): Boolean {
        var ok = false
        onActivity { a ->
            val v = a.findViewById<View>(id); val r = Rect()
            ok = v.isShown && v.getGlobalVisibleRect(r) && r.height() == v.height && r.width() == v.width
        }
        return ok
    }

    private fun text(id: Int): String {
        var t = ""
        onActivity { a -> t = a.findViewById<TextView>(id).text.toString() }
        return t
    }

    /** Thème choisi dans les Réglages, retour à l'Accueil sur « Ce mois », puis trois captures par défilement. */
    private fun captureAccueil(theme: String, suffix: String): String {
        startMain()
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.rowTheme))
        E2e.chooseTheme(theme)
        waitFor(withId(R.id.rowTheme))
        pressBack()
        E2e.waitForAppReady("E2eRevueAccueilTest.$suffix"); waitFor(withId(R.id.tvTicketCount))
        onView(withId(R.id.btnHomeMonth)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.cardBudget))

        scrollHome { _, _ -> 0 }
        shot("revue_accueil_1_haut_$suffix")
        val amount = text(R.id.tvHeroAmount)

        scrollHome { _, a -> (a.findViewById<View>(R.id.cardBudget).top - 24).coerceAtLeast(0) }
        assertTrue("Carte Budget entièrement visible ($suffix)", fullyVisible(R.id.cardBudget))
        shot("revue_accueil_2_budget_$suffix")

        scrollHome { s, _ -> s.getChildAt(0).height - s.height }
        listOf(R.id.btnScanTicket, R.id.btnAddTicket, R.id.btnHistory).forEach {
            assertTrue("Bouton ${ctx.resources.getResourceEntryName(it)} entièrement visible ($suffix)", fullyVisible(it))
        }
        shot("revue_accueil_3_bas_$suffix")
        return amount
    }

    @Test fun a01_donnees_fictives() {
        assertEquals("Émulateur attendu en français", "fr", Locale.getDefault().language)
        assertTrue("App neuve attendue (aucun ticket)", allTickets().isEmpty())
        SessionManager(ctx).apply { markFirstLaunchDone(); login("Testeur revue") }
        runBlocking { tickets().forEach { AppDatabase.getInstance(ctx).ticketDao().insert(it) } }
        val store = BudgetStore(ctx)
        budgets.forEach { (c, l) -> store.set(c, l) }
        assertEquals(tickets().size, allTickets().size)
    }

    @Test fun a02_accueil_clair_puis_sombre() {
        val before = allTickets(); val budgetsBefore = BudgetStore(ctx).load()
        val clair = captureAccueil("Clair", "clair")
        val sombre = captureAccueil("Sombre", "sombre")
        assertEquals("Même montant affiché dans les deux thèmes", clair, sombre)
        // Préférence par défaut rétablie
        onView(withId(R.id.btnSettings)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.rowTheme))
        E2e.chooseTheme("Système")
        waitFor(withId(R.id.rowTheme))
        assertEquals(before, allTickets()); assertEquals(budgetsBefore, BudgetStore(ctx).load())
    }
}
