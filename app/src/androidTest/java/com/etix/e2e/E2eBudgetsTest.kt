package com.etix.e2e

import android.content.Intent
import android.os.SystemClock
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.BudgetStore
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/** Outils communs aux tests de budgets (lot 7). Données FICTIVES. */
object BudgetE2e {
    fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("BudgetE2e"); waitFor(withId(R.id.bottomNav))
    }

    fun openCategoriesMonth() {
        onView(withId(R.id.menu_category)).perform(click())
        waitFor(withId(R.id.togglePeriodCategory))
        onView(withId(R.id.btnCatMonth)).perform(click())
    }

    /** Ligne de catégorie (pas la légende de l'anneau) contenant tous les éléments donnés. */
    fun row(vararg m: Matcher<View>) = onView(withId(R.id.recyclerViewCategories)).perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
            allOf((m.toList() + withId(R.id.tvCategoryPercent)).map { hasDescendant(it) })))

    fun noBudgetOn(category: String) = row(withText(category),
        allOf(withId(R.id.budgetBlock), withEffectiveVisibility(Visibility.GONE)))

    /** Réglage → ligne de la catégorie → saisie AU CLAVIER (après ouverture du clavier) → Appliquer (barre haute). */
    fun setBudget(category: String, typed: String) {
        onView(withId(R.id.btnBudgets)).perform(click())
        waitFor(withId(R.id.tvBudgetsTitle))
        onView(allOf(withId(R.id.tvBudgetCategory), withText(category))).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.inputBudget))
        SystemClock.sleep(1500) // clavier ouvert automatiquement : laisser finir l'animation avant de taper
        onView(withId(R.id.inputBudget)).perform(androidx.test.espresso.action.ViewActions.clearText(), typeText(typed))
        onView(withId(R.id.inputBudget)).check(matches(withText(typed)))
        onView(withId(R.id.btnBudgetApply)).perform(click())
    }

    fun settingsValue(category: String, value: String) =
        waitFor(allOf(withId(R.id.tvBudgetValue), androidx.test.espresso.matcher.ViewMatchers.hasSibling(
            allOf(withId(R.id.tvBudgetCategory), withText(category))), withText(value)))

    fun tickets() = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }
}

/** Avant la mise à jour A → B : budget saisi au clavier sur la catégorie du ticket du parcours (« Autre »). */
@RunWith(AndroidJUnit4::class)
class E2eBudgetAvantMajTest {
    @Test fun budget_saisi_avant_mise_a_jour() {
        val before = BudgetE2e.tickets()
        BudgetE2e.startMain()
        BudgetE2e.openCategoriesMonth()
        BudgetE2e.setBudget("Autre", "20,50")
        BudgetE2e.settingsValue("Autre", "20,50 €")
        shot("51_reglage_budget_avant_maj")
        pressBack()
        BudgetE2e.row(withText("Autre"), withText("15,75 € / 20,50 €"))
        shot("52_categories_budget_avant_maj")
        assertEquals(before, BudgetE2e.tickets())
    }
}

/** Après mise à jour A → B par `adb install -r` + processus tué : budget et tickets intacts. */
@RunWith(AndroidJUnit4::class)
class E2eBudgetApresMajTest {
    @Test fun budget_conserve_apres_mise_a_jour_et_redemarrage() {
        assertEquals(20.5, BudgetStore(ctx).limit("Autre")!!, 0.0)
        BudgetE2e.startMain()
        BudgetE2e.openCategoriesMonth()
        BudgetE2e.row(withText("Autre"), withText("15,75 € / 20,50 €"))
        shot("53_categories_budget_apres_maj")
        assertTrue(BudgetE2e.tickets().any { it.store == "Boulangerie Test" && it.amount == 15.75 })
    }
}

/**
 * Budgets sur les catégories fictives « E2E … » (prérequis : E2eCategoriesTest) : dépassement, attention,
 * catégorie sans budget, « Cette année » sans barre, préremplissage, suppression d'UN budget, thème sombre.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eBudgetsTest {

    @Test fun g01_depassement_attention_sans_budget() {
        val before = BudgetE2e.tickets()
        BudgetE2e.startMain()
        BudgetE2e.openCategoriesMonth()
        BudgetE2e.setBudget("E2E Hausse", "15,50")   // 20,00 / 15,50 → 129 %
        BudgetE2e.setBudget("E2E Baisse", "12")      // 10,00 / 12 → 83 %
        pressBack()
        BudgetE2e.row(withText("E2E Hausse"), withText("Dépassé — 129%"), withText("20 € / 15,50 €"))
        shot("54_budget_depasse")
        BudgetE2e.row(withText("E2E Baisse"), withText("Attention — 83%"), withText("10 € / 12 €"))
        BudgetE2e.noBudgetOn("E2E Nouvelle")
        shot("55_budget_attention_sans_budget")

        onView(withId(R.id.recyclerViewCategories)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
        onView(withId(R.id.btnCatYear)).perform(click())
        BudgetE2e.noBudgetOn("E2E Hausse")        // budgets : vue mensuelle uniquement
        onView(withId(R.id.btnCatMonth)).perform(click())
        assertEquals(before, BudgetE2e.tickets())
    }

    @Test fun g02_prerempli_puis_suppression_d_un_budget() {
        BudgetE2e.startMain()
        BudgetE2e.openCategoriesMonth()
        onView(withId(R.id.btnBudgets)).perform(click())
        onView(allOf(withId(R.id.tvBudgetCategory), withText("E2E Hausse"))).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.inputBudget))
        onView(withId(R.id.inputBudget)).check(matches(withText("15,50")))
        shot("56_saisie_budget_preremplie")
        onView(withId(R.id.btnBudgetCancel)).perform(click())

        onView(allOf(withId(R.id.tvBudgetCategory), withText("E2E Baisse"))).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.inputBudget))
        onView(withId(R.id.btnDeleteBudget)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
        BudgetE2e.settingsValue("E2E Baisse", "—")
        BudgetE2e.settingsValue("E2E Hausse", "15,50 €")   // l'autre budget n'est pas touché
        assertNull(BudgetStore(ctx).limit("E2E Baisse"))
        pressBack()
        BudgetE2e.noBudgetOn("E2E Baisse")
    }

    @Test fun g03_theme_sombre() {
        BudgetE2e.startMain()
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnToggleTheme)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        pressBack()
        E2e.waitForAppReady("E2eBudgetsTest.g03"); waitFor(withId(R.id.tvTicketCount))
        BudgetE2e.openCategoriesMonth()
        BudgetE2e.row(withText("E2E Hausse"), withText("Dépassé — 129%"))
        shot("57_budgets_sombre")
        onView(withId(R.id.btnBudgets)).perform(click())
        waitFor(withId(R.id.tvBudgetsTitle))
        shot("58_reglage_budgets_sombre")
        pressBack()
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnToggleTheme)).perform(click())   // retour au clair
        waitFor(withId(R.id.btnToggleTheme))
    }
}
