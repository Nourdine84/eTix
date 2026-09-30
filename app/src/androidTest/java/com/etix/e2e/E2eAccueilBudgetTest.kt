package com.etix.e2e

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.etix.R
import com.etix.data.BudgetStore
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.features.budget.BudgetSummaryEngine
import com.etix.features.budget.HomeBudgetSummary
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/** Outils : carte Budget de l'Accueil (lot 8), attendus calculés sur les données fictives réellement présentes. */
object AccueilBudgetE2e {
    fun expected(): HomeBudgetSummary? = BudgetSummaryEngine.compute(BudgetE2e.tickets(), BudgetStore(ctx).load())

    /** Accueil sur « Ce mois », carte amenée à l'écran, textes comparés au moteur. */
    fun checkCard(shotName: String) {
        val s = expected()
        assertNotNull("Des budgets fictifs doivent exister pour ce test", s)
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnHomeMonth)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.tvTicketCount))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val act = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first()
            val card = act.findViewById<View>(R.id.cardBudget)
            act.findViewById<NestedScrollView>(R.id.homeScroll).scrollTo(0, (card.top - 24).coerceAtLeast(0))
        }
        waitFor(allOf(withId(R.id.tvBudgetHeadline), withText(BudgetSummaryEngine.headline(s!!))))
        waitFor(allOf(withId(R.id.tvBudgetCaption), withText(BudgetSummaryEngine.caption(s))))
        waitFor(allOf(withId(R.id.tvBudgetPercent), withText(BudgetSummaryEngine.percent(s.globalRatio))))
        waitFor(allOf(withId(R.id.tvBudgetDaysLeft), withText(BudgetSummaryEngine.daysLeft(s.daysLeftInMonth))))
        shot(shotName)
    }
}

/**
 * Lot 8 sur émulateur : carte Budget de l'Accueil et phrase « budget tendu » (prérequis : budgets fictifs créés par
 * E2eBudgetsTest, dont un budget partagé « E2E Casse » / « e2e casse »). Lecture seule.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eAccueilBudgetTest {

    @Test fun h01_carte_budget_ce_mois() {
        val before = BudgetE2e.tickets(); val budgetsBefore = BudgetStore(ctx).load()
        BudgetE2e.startMain()
        AccueilBudgetE2e.checkCard("61_accueil_carte_budget")
        val s = AccueilBudgetE2e.expected()!!
        // Budget partagé compté une fois : la ligne « E2E Casse » cumule 30 + 20 = 50 € pour 40 € → 125 %
        val casse = BudgetSummaryEngine.compute(BudgetE2e.tickets(), mapOf("e2e casse" to 40.0))!!.lines.single()
        assertEquals(50.0, casse.spent, 0.0)
        // Phrase de l'Accueil : calculée comme l'app (maturité des données d'abord, puis « budget tendu »)
        val snap = com.etix.features.home.HomeSnapshot.of(BudgetE2e.tickets(), com.etix.features.store.TimeRange.MONTH)
        val expected = com.etix.features.home.HomeCopy.narration(
            com.etix.features.home.FinancialStateEngine.evaluate(snap, budgetTense = s.isTense),
            com.etix.features.store.TimeRange.MONTH)
        if (s.isTense && snap.hasComparison) assertEquals("Ton rythme de dépenses augmente", expected)
        onView(withId(R.id.tvNarration)).perform(E2e.nestedScrollTo())
        waitFor(allOf(withId(R.id.tvNarration), withText(expected)))
        assertEquals(before, BudgetE2e.tickets()); assertEquals(budgetsBefore, BudgetStore(ctx).load())
    }

    @Test fun h02_cette_annee_sans_carte() {
        BudgetE2e.startMain()
        onView(withId(R.id.btnHomeYear)).perform(click())
        waitFor(withId(R.id.tvTicketCount))
        onView(withId(R.id.cardBudget)).check(androidx.test.espresso.assertion.ViewAssertions.matches(
            androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(
                androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
        shot("62_accueil_annee_sans_carte")
        onView(withId(R.id.btnHomeMonth)).perform(click())
    }

    @Test fun h03_theme_sombre() {
        BudgetE2e.startMain()
        onView(withId(R.id.btnSettings)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnToggleTheme)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        pressBack()
        E2e.waitForAppReady("E2eAccueilBudgetTest.h03"); waitFor(withId(R.id.tvTicketCount))
        AccueilBudgetE2e.checkCard("63_accueil_carte_budget_sombre")
        onView(withId(R.id.btnSettings)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnToggleTheme)).perform(click())   // retour au clair
        waitFor(withId(R.id.btnToggleTheme))
    }
}
