package com.etix.e2e

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.Calendar
import java.util.Locale

/**
 * Lot 5 sur émulateur : onglet Catégories (iOS CategoryView). Données FICTIVES préfixées « E2E », ajoutées aux
 * données des phases précédentes (rien n'est supprimé ni modifié). Vérifie : lignes par catégorie exacte,
 * variation vs période précédente (hausse / baisse / absente), périodes Aujourd'hui / Ce mois / Cette année,
 * thème sombre, et que l'écran ne modifie aucun ticket.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eCategoriesTest {

    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()
    private val now = System.currentTimeMillis()
    private val prevMonthMid: Long = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 10); set(Calendar.HOUR_OF_DAY, 12)
    }.timeInMillis
    private val prevMonthStart: Long = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    /** 2 janvier de l'année courante, midi : dans « Cette année », hors mois courant et mois précédent (dès mars). */
    private val earlyYear: Long = Calendar.getInstance().apply {
        set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 2); set(Calendar.HOUR_OF_DAY, 12)
    }.timeInMillis

    private fun pct(v: Double) = String.format(Locale.FRANCE, "%+.0f%%", v)

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eCategoriesTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun openCategories() {
        onView(withId(R.id.menu_category)).perform(click())
        waitFor(withId(R.id.togglePeriodCategory))
    }

    private fun row(vararg m: Matcher<View>) =
        onView(withId(R.id.recyclerViewCategories)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                // Lignes uniquement : la légende de la carte anneau contient aussi les noms
                allOf((m.toList() + withId(R.id.tvCategoryPercent)).map { hasDescendant(it) })))

    private fun noRow(name: String) {
        try { row(withText(name)); throw AssertionError("« $name » ne devrait pas figurer sur cette période") }
        catch (expected: PerformException) { /* absent */ }
    }

    private fun top() = onView(withId(R.id.recyclerViewCategories)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))

    @Test
    fun d01_mois_repartition_et_variations() = runBlocking {
        val before = dao.getAllFlow().first()
        listOf(
            Ticket(store = "E2E Magasin", amount = 10.0, category = "E2E Hausse", dateMillis = prevMonthMid),
            Ticket(store = "E2E Magasin", amount = 20.0, category = "E2E Hausse", dateMillis = now - 60_000),
            Ticket(store = "E2E Magasin", amount = 40.0, category = "E2E Baisse", dateMillis = prevMonthMid),
            Ticket(store = "E2E Magasin", amount = 10.0, category = "E2E Baisse", dateMillis = now - 60_000),
            Ticket(store = "E2E Magasin", amount = 7.5, category = "E2E Nouvelle", dateMillis = now - 60_000),
            Ticket(store = "E2E Magasin", amount = 3.0, category = "E2E Ancien", dateMillis = earlyYear),
        ).forEach { dao.insert(it) }

        startMain()
        openCategories()
        onView(withId(R.id.btnCatMonth)).perform(click())
        waitFor(withId(R.id.tvDonutTotal))
        shot("40_categories_mois")

        row(withText("E2E Hausse"), withText("20,00 €"), withText(pct(100.0)))
        row(withText("E2E Baisse"), withText("10,00 €"), withText(pct(-75.0)))
        row(withText("E2E Nouvelle"), withText("7,50 €"), allOf(withId(R.id.tvCategoryDelta), withText("")))
        if (earlyYear < prevMonthStart) noRow("E2E Ancien")

        // L'écran n'a rien modifié : tickets antérieurs intacts + 6 ajoutés
        val after = dao.getAllFlow().first()
        assertEquals(before.size + 6, after.size)
        assertEquals(before.sortedBy { it.id }, after.filter { a -> before.any { it.id == a.id } }.sortedBy { it.id })
    }

    @Test
    fun d02_annee_et_aujourdhui() {
        startMain()
        openCategories()
        onView(withId(R.id.btnCatYear)).perform(click())
        waitFor(withId(R.id.tvDonutTotal))
        assumeTrue("Janvier/février : pas de jour de l'année hors mois précédent", earlyYear < prevMonthStart)
        row(withText("E2E Ancien"), withText("3,00 €"))
        top()
        shot("41_categories_annee")

        onView(withId(R.id.btnCatToday)).perform(click())
        row(withText("E2E Nouvelle"))
        noRow("E2E Ancien")
        top()
        shot("42_categories_aujourdhui")
        onView(withId(R.id.btnCatMonth)).perform(click())
    }

    @Test
    fun d03_theme_sombre_puis_clair() {
        startMain()
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnToggleTheme)).perform(click())   // → sombre (recréation)
        waitFor(withId(R.id.btnToggleTheme))
        pressBack()
        waitFor(withId(R.id.tvTicketCount))
        openCategories()
        onView(withId(R.id.btnCatMonth)).perform(click())
        waitFor(withId(R.id.tvDonutTotal))
        shot("43_categories_sombre")
        row(withText("E2E Baisse"))
        shot("44_categories_sombre_lignes")

        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnToggleTheme)).perform(click())   // → clair (préférence rétablie)
        waitFor(withId(R.id.btnToggleTheme))
    }
}
