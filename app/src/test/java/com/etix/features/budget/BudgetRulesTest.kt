package com.etix.features.budget

import com.etix.features.category.CategoryStats
import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals("Dépassé — 100%", BudgetRules.statusLabel(exact)) // iOS : ≥ 100 % = dépassé
        assertEquals(0.0, exact.remaining, 0.0)

        val over = BudgetLine(limit = 15.5, spent = 20.0)
        assertEquals("Dépassé — 129%", BudgetRules.statusLabel(over))
        assertEquals(-4.5, over.remaining, 1e-9)
        assertEquals(4.5, over.overrun, 1e-9)
        assertEquals(1.0, BudgetRules.progress(over), 0.0)
        assertEquals("20 € / 15,50 €", BudgetRules.progressLabel(over))
        assertEquals("Budget 15,50 €, dépassé de 4,50 €", BudgetRules.accessibilityText(over))
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
     * Deux catégories ne différant que par la casse (« Courses » / « courses ») : UNE clé de budget (iOS), mais DEUX
     * lignes de dépenses séparées dans Catégories. Budget 40 €, dépenses 30 € + 20 € = 50 € : aucune ligne n'est en
     * dépassement (75 % et 50 %) alors que le total réel dépasse le budget. Comportement iOS conservé, documenté
     * (docs/BUDGETS.md, A2) ; aucune catégorie fusionnée ni renommée.
     */
    @Test fun deux_categories_differant_par_la_casse_partagent_le_budget_pas_les_depenses() {
        val now = Calendar.getInstance().apply { clear(); set(2026, 8, 15, 12, 0) }.timeInMillis
        val tickets = listOf(
            Ticket(id = 1, store = "A", amount = 30.0, category = "Courses", dateMillis = now - 3_600_000),
            Ticket(id = 2, store = "B", amount = 20.0, category = "courses", dateMillis = now - 7_200_000),
        )
        val budgets = mapOf(BudgetRules.key("Courses") to 40.0)
        val rows = CategoryStats.breakdown(tickets, TimeRange.MONTH, now).categories
        assertEquals(listOf("Courses", "courses"), rows.map { it.name })         // deux lignes, noms intacts
        val lines = rows.map { BudgetLine(BudgetRules.limitForRow(budgets, it.name, true)!!, it.total) }
        assertEquals(listOf(BudgetStatus.OK, BudgetStatus.OK), lines.map { it.status }) // 75 % et 50 %
        assertEquals(listOf(0.75, 0.5), lines.map { it.ratio })
        // Total réel de la clé « courses » (agrégat de l'Accueil iOS, non porté) : 50 € / 40 € → dépassé
        val combined = BudgetLine(40.0, tickets.sumOf { it.amount })
        assertEquals(BudgetStatus.EXCEEDED, combined.status)
    }
}
