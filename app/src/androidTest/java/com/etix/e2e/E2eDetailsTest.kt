package com.etix.e2e

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.nestedScrollTo
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Lot 6 sur émulateur : détail d'une catégorie et d'un ticket (iOS CategoryDetailView / TicketDetailView),
 * navigation Retour, suppression avec confirmation (Annuler n'efface rien ; Supprimer n'efface que CE ticket
 * fictif créé par le test), thème sombre. Prérequis : E2eCategoriesTest (catégories « E2E … » fictives).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eDetailsTest {

    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()
    private fun inOverlay(id: Int): Matcher<View> = allOf(withId(id), isDescendantOfA(withId(R.id.overlayContainer)))

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eDetailsTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun openCategory(name: String) {
        onView(withId(R.id.menu_category)).perform(click())
        onView(withId(R.id.btnCatMonth)).perform(click())
        onView(withId(R.id.recyclerViewCategories)).perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
            allOf(hasDescendant(withText(name)), hasDescendant(withId(R.id.tvCategoryPercent))), click()))
        waitFor(allOf(inOverlay(R.id.tvCategoryDetailTitle), withText(name)))
    }

    @Test
    fun e01_categorie_puis_ticket_puis_retour() {
        startMain()
        openCategory("E2E Baisse")
        waitFor(allOf(inOverlay(R.id.tvCategoryDetailTotal), withText("10,00 €")))
        onView(inOverlay(R.id.tvCategoryDetailCount)).check(matches(withText("1 ticket")))
        waitFor(inOverlay(R.id.dayChart))
        shot("45_categorie_detail")

        onView(allOf(withId(R.id.tvCatTicketStore), withText("E2E Magasin"))).perform(nestedScrollTo(), click())
        waitFor(allOf(inOverlay(R.id.tvAmount), withText("10,00 €")))
        onView(inOverlay(R.id.tvCategory)).check(matches(withText("E2E Baisse")))
        onView(inOverlay(R.id.tvStoreValue)).check(matches(withText("E2E Magasin")))
        shot("46_ticket_detail")

        pressBack()
        waitFor(allOf(inOverlay(R.id.tvCategoryDetailTotal), withText("10,00 €")))
        pressBack()
        waitFor(withId(R.id.togglePeriodCategory))
        onView(withId(R.id.overlayContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun e02_suppression_confirmee_depuis_le_detail() {
        runBlocking { dao.insert(Ticket(store = "E2E A supprimer", amount = 1.23, category = "E2E Suppression",
            dateMillis = System.currentTimeMillis() - 30_000)) }
        val before = runBlocking { dao.getAllFlow().first() }
        startMain()
        openCategory("E2E Suppression")
        onView(allOf(withId(R.id.tvCatTicketStore), withText("E2E A supprimer"))).perform(nestedScrollTo(), click())
        waitFor(allOf(inOverlay(R.id.tvAmount), withText("1,23 €")))

        onView(inOverlay(R.id.btnDeleteTicket)).perform(nestedScrollTo(), click())
        waitFor(withText("Supprimer ce ticket ?"))
        shot("47_confirmation_suppression")
        onView(withText("Annuler")).inRoot(isDialog()).perform(click())
        waitFor(allOf(inOverlay(R.id.tvAmount), withText("1,23 €")))
        assertEquals("Annuler ne doit rien supprimer", before.size, runBlocking { dao.getAllFlow().first().size })

        onView(inOverlay(R.id.btnDeleteTicket)).perform(nestedScrollTo(), click())
        onView(withText("Supprimer")).inRoot(isDialog()).perform(click())
        // Retour au détail de la catégorie, désormais vide
        waitFor(allOf(inOverlay(R.id.tvCategoryDetailCount), withText("0 ticket")))
        waitFor(inOverlay(R.id.emptyCategoryDetail))
        shot("48_categorie_apres_suppression")
        val after = runBlocking { dao.getAllFlow().first() }
        assertEquals(before.size - 1, after.size)
        assertTrue(after.none { it.store == "E2E A supprimer" })
        assertEquals(before.filter { it.store != "E2E A supprimer" }.sortedBy { it.id }, after.sortedBy { it.id })
        pressBack()
        waitFor(withId(R.id.togglePeriodCategory))
    }

    @Test
    fun e03_theme_sombre() {
        startMain()
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.rowTheme))
        E2e.chooseTheme("Sombre")   // → sombre
        waitFor(withId(R.id.rowTheme))
        pressBack()
        waitFor(withId(R.id.tvTicketCount))
        openCategory("E2E Hausse")
        waitFor(inOverlay(R.id.dayChart))
        shot("49_categorie_detail_sombre")
        onView(allOf(withId(R.id.tvCatTicketStore), withText("E2E Magasin"))).perform(nestedScrollTo(), click())
        waitFor(allOf(inOverlay(R.id.tvAmount), withText("20,00 €")))
        shot("50_ticket_detail_sombre")
        pressBack(); pressBack()
        waitFor(withId(R.id.togglePeriodCategory))

        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.rowTheme))
        E2e.chooseTheme("Système")   // → Système (préférence par défaut rétablie)
        waitFor(withId(R.id.rowTheme))
    }
}
