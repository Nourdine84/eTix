package com.etix.features.budget

import com.etix.features.category.CategoryStats
import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BudgetRulesTest {

    @Test fun cle_en_minuscules_comme_ios() {
        assertEquals("courses", BudgetRules.key("Courses"))
        assertEquals("santé", BudgetRules.key("SANTÉ"))
        assertEquals(" autre ", BudgetRules.key(" Autre ")) // iOS ne retire pas les espaces
    }

    @Test fun saisie_montant_francais() {
        assertEquals(300.0, BudgetRules.parseLimit("300")!!, 0.0)
        assertEquals(12.5, BudgetRules.parseLimit("12,50")!!, 0.0)
        assertEquals(12.5, BudgetRules.parseLimit("12.50")!!, 0.0)
        assertEquals(1234.5, BudgetRules.parseLimit("1 234,50")!!, 0.0)
        assertEquals(0.2, BudgetRules.parseLimit(",20")!!, 1e-9) // décision « ,20 » en attente
        assertNull(BudgetRules.parseLimit("0"))
        assertNull(BudgetRules.parseLimit("0,00"))
        assertNull(BudgetRules.parseLimit("-5"))
        assertNull(BudgetRules.parseLimit(""))
        assertNull(BudgetRules.parseLimit("abc"))
        assertNull(BudgetRules.parseLimit("12,50,3"))
    }

    @Test fun seuils_ios_80_et_100() {
        assertEquals(BudgetStatus.OK, BudgetRules.status(0.0))
        assertEquals(BudgetStatus.OK, BudgetRules.status(0.7999))
        assertEquals(BudgetStatus.WARNING, BudgetRules.status(0.8))
        assertEquals(BudgetStatus.WARNING, BudgetRules.status(0.9999))
        assertEquals(BudgetStatus.EXCEEDED, BudgetRules.status(1.0))
        assertEquals(BudgetStatus.EXCEEDED, BudgetRules.status(2.5))
    }

    @Test fun libelles_restant_et_depassement() {
        val ok = BudgetLine(limit = 100.0, spent = 40.0)
        assertNull(BudgetRules.statusLabel(ok))
        assertEquals(60.0, ok.remaining, 0.0)
        assertEquals("Budget 100 €, reste 60 €", BudgetRules.accessibilityText(ok))

        val warn = BudgetLine(limit = 12.0, spent = 10.0)
        assertEquals("Attention — 83%", BudgetRules.statusLabel(warn))

        val exact = BudgetLine(limit = 50.0, spent = 50.0)
        // Décision du 09/10/2026 (écart volontaire avec iOS, qui affiche « Dépassé — 100% ») : budget atteint
        assertEquals("Budget atteint", BudgetRules.statusLabel(exact))
        assertEquals(BudgetStatus.EXCEEDED, exact.status) // état (couleur) inchangé à 100 %
        assertEquals("Budget 50 €, reste 0 €", BudgetRules.accessibilityText(exact))
        assertEquals(0.0, exact.remaining, 0.0)

        val over = BudgetLine(limit = 15.5, spent = 20.0)
        assertEquals("Dépassé — 129%", BudgetRules.statusLabel(over))
        assertEquals(-4.5, over.remaining, 1e-9)
        assertEquals(4.5, over.overrun, 1e-9)
        assertEquals(1.0, BudgetRules.progress(over), 0.0)
        assertEquals("20 € / 15,50 €", BudgetRules.progressLabel(over))
        assertEquals("Budget 15,50 €, dépassé de 4,50 €", BudgetRules.accessibilityText(over))
    }

    /** Montants exacts au centime, pas le pourcentage arrondi (décision du 09/10/2026). */
    @Test fun budget_atteint_compare_les_montants_exacts() {
        val sous = BudgetLine(limit = 60.0, spent = 59.99)        // 99,98 % : « 100% » une fois arrondi
        assertEquals(BudgetStatus.WARNING, sous.status)
        assertEquals("Attention — 100%", BudgetRules.statusLabel(sous))
        assertEquals("Budget 60 €, reste 0,01 €", BudgetRules.accessibilityText(sous))

        val egal = BudgetLine(limit = 60.0, spent = 60.0)
        assertEquals("Budget atteint", BudgetRules.statusLabel(egal))

        val dessus = BudgetLine(limit = 60.0, spent = 60.01)      // 100,02 % : « 100% » une fois arrondi
        assertEquals(BudgetStatus.EXCEEDED, dessus.status)
        assertEquals("Dépassé — 100%", BudgetRules.statusLabel(dessus))
        assertEquals("Budget 60 €, dépassé de 0,01 €", BudgetRules.accessibilityText(dessus))

        // Restes d'arrondi des sommes : 0,1 + 0,2 € dépensés pour 0,30 € de budget = atteint (ni 99 %, ni dépassé)
        val somme = BudgetLine(limit = 0.3, spent = 0.1 + 0.2)
        assertTrue(somme.ratio > 1.0)
        assertEquals("Budget atteint", BudgetRules.statusLabel(somme))
        val sommeSous = BudgetLine(limit = 0.3, spent = 0.29999999999)
        assertEquals("Budget atteint", BudgetRules.statusLabel(sommeSous))

        // Seuil d'alerte (80 %) inchangé
        assertNull(BudgetRules.statusLabel(BudgetLine(limit = 100.0, spent = 79.99)))
        assertEquals("Attention — 80%", BudgetRules.statusLabel(BudgetLine(limit = 100.0, spent = 80.0)))
    }

    /** Budget partagé (casse) : consommation cumulée comparée au budget, au centime. */
    @Test fun budget_partage_atteint_et_depasse() {
        val atteint = BudgetRules.rowBudgets(listOf("Courses" to 30.0, "courses" to 10.0), mapOf("courses" to 40.0), true)
        assertEquals("Budget atteint", BudgetRules.statusLabel(atteint.getValue("Courses").line))
        assertEquals("Budget atteint", BudgetRules.statusLabel(atteint.getValue("courses").line))
        val depasse = BudgetRules.rowBudgets(listOf("Courses" to 30.0, "courses" to 10.01), mapOf("courses" to 40.0), true)
        assertEquals("Dépassé — 100%", BudgetRules.statusLabel(depasse.getValue("Courses").line))
    }

    @Test fun affichage_des_montants() {
        assertEquals("300 €", BudgetRules.formatEuro(300.0))
        assertEquals("12,50 €", BudgetRules.formatEuro(12.5))
        assertEquals("12,50", BudgetRules.formatNumber(12.5))
        assertEquals("1234,56 €", BudgetRules.formatEuro(1234.56))
    }

    @Test fun categories_de_la_liste_de_reglage() {
        assertEquals(listOf("Autre", "Courses", "Santé", "courses"),
            BudgetRules.settingsCategories(listOf("Courses", "", "Santé", "courses", "Courses", "Autre")))
    }

    @Test fun budget_seulement_sur_ce_mois() {
        val b = mapOf("courses" to 100.0, "zero" to 0.0)
        assertEquals(100.0, BudgetRules.limitForRow(b, "Courses", isMonth = true)!!, 0.0)
        assertEquals(100.0, BudgetRules.limitForRow(b, "courses", isMonth = true)!!, 0.0) // même clé
        assertNull(BudgetRules.limitForRow(b, "Courses", isMonth = false))
        assertNull(BudgetRules.limitForRow(b, "Loisirs", isMonth = true))            // catégorie sans budget
        assertNull(BudgetRules.limitForRow(b, "zero", isMonth = true))
    }

    /** Changement de mois : les dépenses du mois précédent ne comptent plus, le budget (mensuel) reste. */
    @Test fun changement_de_mois() {
        fun at(y: Int, m: Int, d: Int, h: Int = 12, mi: Int = 0, s: Int = 0, ms: Int = 0) =
            Calendar.getInstance().apply { clear(); set(y, m, d, h, mi, s); set(Calendar.MILLISECOND, ms) }.timeInMillis
        val tickets = listOf(
            Ticket(id = 1, store = "A", amount = 90.0, category = "Courses", dateMillis = at(2026, 2, 31, 23, 59, 59, 999)),
            Ticket(id = 2, store = "B", amount = 30.0, category = "Courses", dateMillis = at(2026, 3, 1, 0, 0, 0, 0)),
        )
        val budgets = mapOf("courses" to 100.0)
        fun spentOn(now: Long) = CategoryStats.breakdown(tickets, TimeRange.MONTH, now).categories
            .firstOrNull { BudgetRules.limitForRow(budgets, it.name, true) != null }?.total
        // 31 mars 23:59 : 90 € dépensés → 90 %
        val march = spentOn(at(2026, 2, 31, 23, 59))!!
        assertEquals(BudgetStatus.WARNING, BudgetLine(100.0, march).status)
        // 1er avril : seul le ticket d'avril compte → 30 %
        val april = spentOn(at(2026, 3, 1, 8))!!
        assertEquals(30.0, april, 0.0)
        assertEquals(BudgetStatus.OK, BudgetLine(100.0, april).status)
        // 1er mai : aucun ticket → pas de ligne (le budget reste enregistré, invisible dans Catégories comme sur iOS)
        assertNull(spentOn(at(2026, 4, 1, 8)))
    }

    /**
     * Deux catégories ne différant que par la casse partagent UN budget : la consommation est CUMULÉE
     * (30 € + 20 € = 50 € pour 40 € → 125 %, dépassé) sur chaque ligne ; les totaux individuels et les noms restent.
     */
    @Test fun budget_partage_par_la_casse_consommation_cumulee() {
        val now = Calendar.getInstance().apply { clear(); set(2026, 8, 15, 12, 0) }.timeInMillis
        val tickets = listOf(
            Ticket(id = 1, store = "A", amount = 30.0, category = "Courses", dateMillis = now - 3_600_000),
            Ticket(id = 2, store = "B", amount = 20.0, category = "courses", dateMillis = now - 7_200_000),
            Ticket(id = 3, store = "C", amount = 10.0, category = "Loisirs", dateMillis = now - 7_200_000),
        )
        val budgets = mapOf(BudgetRules.key("Courses") to 40.0, "loisirs" to 100.0)
        val rows = CategoryStats.breakdown(tickets, TimeRange.MONTH, now).categories
        assertEquals(listOf("Courses", "courses", "Loisirs"), rows.map { it.name })     // aucune fusion, noms intacts
        assertEquals(listOf(30.0, 20.0, 10.0), rows.map { it.total })                   // totaux individuels
        val rb = BudgetRules.rowBudgets(rows.map { it.name to it.total }, budgets, isMonth = true)
        for (n in listOf("Courses", "courses")) {
            assertEquals(50.0, rb.getValue(n).line.spent, 0.0)
            assertEquals(1.25, rb.getValue(n).line.ratio, 1e-9)
            assertEquals(BudgetStatus.EXCEEDED, rb.getValue(n).line.status)
            assertEquals("Dépassé — 125%", BudgetRules.statusLabel(rb.getValue(n).line))
            assertEquals("50 € / 40 €", BudgetRules.progressLabel(rb.getValue(n).line))
        }
        assertEquals(listOf("courses"), rb.getValue("Courses").sharedWith)
        assertEquals("Budget partagé avec « Courses »", BudgetRules.sharedLabel(rb.getValue("courses")))
        assertFalse(rb.getValue("Loisirs").isShared)
        assertNull(BudgetRules.sharedLabel(rb.getValue("Loisirs")))
        assertEquals(10.0, rb.getValue("Loisirs").line.spent, 0.0)
        assertTrue(BudgetRules.rowBudgets(rows.map { it.name to it.total }, budgets, isMonth = false).isEmpty())
    }

    @Test fun trois_variantes_de_casse_un_seul_budget() {
        val rows = listOf("Courses" to 10.0, "courses" to 5.0, "COURSES" to 5.0)
        val rb = BudgetRules.rowBudgets(rows, mapOf("courses" to 10.0), true)
        assertEquals(3, rb.size)
        rb.values.forEach { assertEquals(20.0, it.line.spent, 0.0); assertEquals(2, it.sharedWith.size) }
        assertEquals("Budget partagé avec « courses », « COURSES »", BudgetRules.sharedLabel(rb.getValue("Courses")))
    }

    /** Agrégation des budgets : un budget partagé n'est compté qu'UNE fois ; catégories sans budget exclues. */
    @Test fun agregation_budget_partage_compte_une_seule_fois() {
        val rows = listOf("Courses" to 30.0, "courses" to 20.0, "Loisirs" to 10.0, "Transport" to 99.0)
        val t = BudgetRules.totals(rows, mapOf("courses" to 40.0, "loisirs" to 100.0))
        assertEquals(2, t.budgetCount)
        assertEquals(140.0, t.totalBudget, 0.0)   // 40 + 100, pas 40 + 40 + 100
        assertEquals(60.0, t.totalSpent, 0.0)     // 50 (courses cumulé) + 10 ; Transport sans budget exclu
        assertEquals(80.0, t.remaining, 0.0)
        // Budget sans dépense ce mois : compté dans le total des budgets, 0 dépensé
        val t2 = BudgetRules.totals(emptyList(), mapOf("courses" to 40.0))
        assertEquals(40.0, t2.totalBudget, 0.0); assertEquals(0.0, t2.totalSpent, 0.0)
    }
}
