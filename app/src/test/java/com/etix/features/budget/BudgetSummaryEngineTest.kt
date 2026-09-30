package com.etix.features.budget

import com.etix.features.home.FinancialStateEngine
import com.etix.features.home.FinancialStateKind
import com.etix.features.home.HomeSnapshot
import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BudgetSummaryEngineTest {

    private fun at(y: Int, m: Int, d: Int, h: Int = 12, mi: Int = 0, s: Int = 0, ms: Int = 0) =
        Calendar.getInstance().apply { clear(); set(y, m, d, h, mi, s); set(Calendar.MILLISECOND, ms) }.timeInMillis
    private val now = at(2026, Calendar.SEPTEMBER, 15)
    private var id = 0L
    private fun t(cat: String, amount: Double, date: Long = now - 3_600_000) =
        Ticket(id = ++id, store = "S", amount = amount, category = cat, dateMillis = date)

    @Test fun sans_budget_pas_de_carte() {
        assertNull(BudgetSummaryEngine.compute(listOf(t("Courses", 10.0)), emptyMap(), now))
        assertNull(BudgetSummaryEngine.compute(listOf(t("Courses", 10.0)), mapOf("courses" to 0.0), now))
    }

    @Test fun etats_et_seuils_ios_50_80_100() {
        fun state(spent: Double) = BudgetSummaryEngine.compute(listOf(t("Courses", spent)), mapOf("courses" to 100.0), now)!!.state
        assertEquals(HomeBudgetState.COMFORTABLE, state(49.99))
        assertEquals(HomeBudgetState.CAUTION, state(50.0))    // au seuil 50 %
        assertEquals(HomeBudgetState.CAUTION, state(79.99))
        assertEquals(HomeBudgetState.CRITICAL, state(80.0))   // au seuil d'alerte 80 %
        assertEquals(HomeBudgetState.CRITICAL, state(99.99))
        assertEquals(HomeBudgetState.EXCEEDED, state(100.0))  // 100 % pile = dépassé (iOS)
        assertEquals(HomeBudgetState.EXCEEDED, state(150.0))
    }

    @Test fun textes_de_la_carte() {
        val under = BudgetSummaryEngine.compute(listOf(t("Courses", 40.0)), mapOf("courses" to 100.0), now)!!
        assertEquals("Il te reste 60 €", BudgetSummaryEngine.headline(under))
        assertEquals("40 € dépensés sur 100 € prévus", BudgetSummaryEngine.caption(under))
        assertEquals("40%", BudgetSummaryEngine.percent(under.globalRatio))
        val over = BudgetSummaryEngine.compute(listOf(t("Courses", 112.5)), mapOf("courses" to 100.0), now)!!
        assertEquals("Budgets dépassés de 12,50 €", BudgetSummaryEngine.headline(over))
        assertEquals("112%", BudgetSummaryEngine.percent(over.globalRatio))
        // Au seuil exact (100 %) : iOS affiche « dépassés de 0 € » — reproduit, signalé (docs/BUDGETS.md B3)
        val exact = BudgetSummaryEngine.compute(listOf(t("Courses", 100.0)), mapOf("courses" to 100.0), now)!!
        assertEquals("Budgets dépassés de 0 €", BudgetSummaryEngine.headline(exact))
        // Troncature iOS : 99,99 % → « 99% »
        assertEquals("99%", BudgetSummaryEngine.percent(0.9999))
        assertEquals("1 jour restant dans le mois", BudgetSummaryEngine.daysLeft(1))
        assertEquals("16 jours restants dans le mois", BudgetSummaryEngine.daysLeft(16))
        assertEquals("et 1 autre →", BudgetSummaryEngine.more(1))
        assertEquals("et 2 autres →", BudgetSummaryEngine.more(2))
    }

    /** Budget partagé par la casse : compté UNE fois, dépenses cumulées (30 + 20 = 50 pour 40 → 125 %). */
    @Test fun budget_partage_compte_une_fois_depenses_cumulees() {
        val s = BudgetSummaryEngine.compute(listOf(
            t("Courses", 30.0, now - 1_000), t("courses", 20.0, now - 2_000), t("Loisirs", 10.0),
            t("Transport", 99.0)   // sans budget : exclu des dépenses
        ), mapOf("courses" to 40.0, "loisirs" to 100.0), now)!!
        assertEquals(140.0, s.totalBudget, 0.0)    // 40 + 100, pas 40 + 40 + 100
        assertEquals(60.0, s.totalSpent, 0.0)      // 50 + 10
        assertEquals(2, s.lines.size)
        val courses = s.lines.first()
        assertEquals("Courses", courses.categoryName) // casse du ticket le plus récent
        assertEquals(50.0, courses.spent, 0.0)
        assertEquals(1.25, courses.ratio, 1e-9)
        assertEquals(HomeBudgetState.EXCEEDED, courses.state)
        // État GLOBAL (iOS) : 60 / 140 = 43 % → confortable, donc pas « budget tendu », malgré une ligne à 125 %
        assertEquals(HomeBudgetState.COMFORTABLE, s.state)
        assertFalse(s.isTense)
    }

    @Test fun changement_de_mois_et_jours_restants() {
        val tickets = listOf(t("Courses", 90.0, at(2026, 7, 31, 23, 59, 59, 999)), t("Courses", 30.0, at(2026, 8, 1, 0)))
        val b = mapOf("courses" to 100.0)
        // 31 août 23:59 : 90 € → critique ; dernier jour du mois → 1 jour restant
        val aug = BudgetSummaryEngine.compute(tickets, b, at(2026, 7, 31, 23, 59))!!
        assertEquals(90.0, aug.totalSpent, 0.0); assertEquals(HomeBudgetState.CRITICAL, aug.state)
        assertEquals(1, aug.daysLeftInMonth)
        // 1er septembre 08:00 : seul le ticket de septembre compte → 30 %, 30 jours restants
        val sep = BudgetSummaryEngine.compute(tickets, b, at(2026, 8, 1, 8))!!
        assertEquals(30.0, sep.totalSpent, 0.0); assertEquals(HomeBudgetState.COMFORTABLE, sep.state)
        assertEquals(30, sep.daysLeftInMonth)
        // Mois sans ticket : carte toujours présente (budget mensuel reconduit), 0 dépensé
        val oct = BudgetSummaryEngine.compute(tickets, b, at(2026, 9, 1, 8))!!
        assertEquals(0.0, oct.totalSpent, 0.0); assertEquals("Il te reste 100 €", BudgetSummaryEngine.headline(oct))
    }

    @Test fun trois_lignes_max_triees_par_ratio_puis_autres() {
        val s = BudgetSummaryEngine.compute(listOf(t("A", 10.0), t("B", 90.0), t("C", 50.0), t("D", 20.0)),
            mapOf("a" to 100.0, "b" to 100.0, "c" to 100.0, "d" to 100.0, "e" to 100.0), now)!!
        assertEquals(listOf("B", "C", "D"), s.lines.map { it.categoryName })
        assertEquals(2, s.extraCount)
    }

    @Test fun budget_sans_ticket_nom_capitalise_et_categorie_vide_ignoree() {
        val s = BudgetSummaryEngine.compute(listOf(t("", 50.0)), mapOf("vie courante" to 10.0), now)!!
        assertEquals("Vie Courante", s.lines.single().categoryName)
        assertEquals(0.0, s.totalSpent, 0.0)
    }

    /** « Budget tendu » (critique ou dépassé) → phrase « rythme de dépenses augmente » même sans hausse forte. */
    @Test fun budget_tendu_sur_la_phrase_de_l_accueil() {
        val snap = HomeSnapshot(periodTotal = 101.0, periodTicketCount = 3, previousPeriodTotal = 100.0, allTimeTicketCount = 10)
        assertEquals(FinancialStateKind.STEADY, FinancialStateEngine.evaluate(snap, budgetTense = false).kind)
        assertEquals(FinancialStateKind.HIGH_SPENDING, FinancialStateEngine.evaluate(snap, budgetTense = true).kind)
        val tense = BudgetSummaryEngine.compute(listOf(t("Courses", 85.0)), mapOf("courses" to 100.0), now)!!
        assertTrue(tense.isTense)
        val calm = BudgetSummaryEngine.compute(listOf(t("Courses", 79.0)), mapOf("courses" to 100.0), now)!!
        assertFalse(calm.isTense)
        // iOS : maturité d'abord — données insuffisantes → BUILDING, jamais HIGH_SPENDING
        val young = HomeSnapshot(0.0, 0, 200.0, 20)
        assertEquals(FinancialStateKind.BUILDING, FinancialStateEngine.evaluate(young, budgetTense = true).kind)
    }
}
