package com.etix.ui.preview

import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import com.etix.testutil.Screens.capture
import com.etix.testutil.Screens.idle
import com.etix.testutil.Screens.waitFor
import com.etix.testutil.TestDb
import com.etix.ui.category.CategoryDetailFragment
import com.etix.ui.detail.TicketDetailFragmentV2
import com.etix.ui.main.MainActivityV2
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.util.concurrent.TimeUnit

/**
 * Lot 6 — détail d'un ticket et détail d'une catégorie (iOS TicketDetailView / CategoryDetailView), clair et sombre,
 * navigation Catégories → catégorie → ticket → Retour → Retour, suppression : Annuler ne supprime rien, Supprimer
 * ne supprime que CE ticket. Rendu Robolectric : aperçu, pas une validation visuelle.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h780dp-hdpi")
class Lot6ScreenshotTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private val day = TimeUnit.DAYS.toMillis(1)
    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()

    private fun seed(): List<Ticket> {
        TestDb.reset(ctx)
        val now = System.currentTimeMillis()
        // Dates dans le mois courant : aujourd'hui uniquement si on est en début de mois
        val dom = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        // Le ticket Lidl doit rester le plus récent (1re ligne touchée par le test), y compris le 1er du mois où
        // tous les tickets tombent le même jour (échec constaté le 01/10/2026) : les autres sont antérieurs d'1 s au moins.
        fun back(n: Int) = now - minOf(n, dom - 1) * day - 1_000
        val list = listOf(
            Ticket(id = 1, store = "Lidl", amount = 64.20, category = "Courses", dateMillis = now,
                description = "Courses de la semaine, promo sur le café"),
            Ticket(id = 2, store = "Carrefour", amount = 42.30, category = "Courses", dateMillis = back(1)),
            Ticket(id = 3, store = "Marché", amount = 12.90, category = "Courses", dateMillis = back(3)),
            Ticket(id = 4, store = "Lidl", amount = 18.60, category = "Courses", dateMillis = back(3) - 3_600_000),
            Ticket(id = 5, store = "Esso", amount = 58.00, category = "Transport", dateMillis = now - 120_000),
        )
        TestDb.seed(ctx, list)
        return list
    }

    private fun launch(): MainActivityV2 = Robolectric.buildActivity(MainActivityV2::class.java).setup().get().also { idle() }
    private fun MainActivityV2.overlay() = findViewById<ViewGroup>(R.id.overlayContainer)
    private fun MainActivityV2.top() = supportFragmentManager.findFragmentById(R.id.overlayContainer)

    private fun openCoursesFromTab(a: MainActivityV2) {
        a.findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.menu_category
        idle()
        val rv = a.findViewById<RecyclerView>(R.id.recyclerViewCategories)
        waitFor { rv.childCount > 1 }
        // Position 1 = 1re ligne (Courses : plus gros total)
        rv.findViewHolderForAdapterPosition(1)!!.itemView.performClick()
        idle()
        waitFor { a.overlay().findViewById<TextView>(R.id.tvCategoryDetailTotal)?.text?.isNotEmpty() == true }
    }

    private fun shoot(suffix: String) {
        val seeded = seed()
        val a = launch()
        openCoursesFromTab(a)
        assertTrue(a.top() is CategoryDetailFragment)
        assertEquals("Courses", a.overlay().findViewById<TextView>(R.id.tvCategoryDetailTitle).text.toString())
        val month = seeded.filter { it.category == "Courses" && it.dateMillis in TimeRange.MONTH.currentRange() }
        assertEquals(String.format(java.util.Locale.FRANCE, "%.2f €", month.sumOf { it.amount }),
            a.overlay().findViewById<TextView>(R.id.tvCategoryDetailTotal).text.toString())
        capture(a, "l6_01_categorie_detail_$suffix")
        a.overlay().findViewById<NestedScrollView>(R.id.categoryDetailScroll).fullScroll(View.FOCUS_DOWN); idle()
        capture(a, "l6_02_categorie_detail_bas_$suffix")

        // Toucher le 1er ticket (Lidl 64,20, avec note) → détail du ticket
        val sections = a.overlay().findViewById<ViewGroup>(R.id.daySections)
        val firstRow = (0 until sections.childCount).map { sections.getChildAt(it) }.first { it !is TextView }
        firstRow.performClick(); idle()
        waitFor { a.overlay().findViewById<TextView>(R.id.tvAmount)?.text?.toString() == "64,20 €" }
        assertTrue(a.top() is TicketDetailFragmentV2)
        assertEquals(View.VISIBLE, a.overlay().findViewById<View>(R.id.cardNote).visibility)
        capture(a, "l6_03_ticket_detail_$suffix")
        a.overlay().findViewById<NestedScrollView>(R.id.ticketDetailScroll).fullScroll(View.FOCUS_DOWN); idle()
        capture(a, "l6_04_ticket_detail_bas_$suffix")

        // Retour → détail catégorie ; Retour → onglet Catégories
        a.onBackPressedDispatcher.onBackPressed(); idle()
        assertTrue(a.top() is CategoryDetailFragment)
        a.onBackPressedDispatcher.onBackPressed(); idle()
        assertEquals(null, a.top())
        assertEquals(MainActivityV2.PAGE_CATEGORY, a.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).currentItem)
    }

    @Test fun details_light() = shoot("light")

    @Test @Config(qualifiers = "+night")
    fun details_dark() = shoot("dark")

    @Test fun ticket_sans_note_et_periode_vide_light() {
        seed()
        val a = launch()
        a.openTicketDetail(5L); idle()
        waitFor { a.overlay().findViewById<TextView>(R.id.tvAmount)?.text?.toString() == "58,00 €" }
        assertEquals(View.GONE, a.overlay().findViewById<View>(R.id.cardNote).visibility)
        capture(a, "l6_05_ticket_sans_note_light")
        a.onBackPressedDispatcher.onBackPressed(); idle()
        a.openCategoryDetail("Transport", TimeRange.TODAY); idle()
        waitFor { a.overlay().findViewById<TextView>(R.id.tvCategoryDetailTotal)?.text?.isNotEmpty() == true }
        a.overlay().findViewById<View>(R.id.btnCatDetailYear).performClick(); idle()
        a.openCategoryDetail("Santé", TimeRange.MONTH); idle()
        waitFor { a.overlay().findViewById<View>(R.id.emptyCategoryDetail)?.visibility == View.VISIBLE }
        assertEquals("0 ticket", a.overlay().findViewById<TextView>(R.id.tvCategoryDetailCount).text.toString())
        capture(a, "l6_06_categorie_vide_light")
    }

    @Test @Config(qualifiers = "+night")
    fun categorie_vide_dark() {
        seed()
        val a = launch()
        a.openCategoryDetail("Santé", TimeRange.MONTH); idle()
        waitFor { a.overlay().findViewById<View>(R.id.emptyCategoryDetail)?.visibility == View.VISIBLE }
        capture(a, "l6_06_categorie_vide_dark")
    }

    @Test fun suppression_depuis_le_detail_annuler_puis_confirmer() {
        seed()
        val a = launch()
        a.openCategoryDetail("Courses", TimeRange.YEAR); idle()
        a.openTicketDetail(3L); idle()
        waitFor { a.overlay().findViewById<TextView>(R.id.tvAmount)?.text?.toString() == "12,90 €" }

        a.overlay().findViewById<View>(R.id.btnDeleteTicket).performClick(); idle()
        val d1 = ShadowDialog.getLatestDialog() as AlertDialog
        assertTrue(d1.isShowing)
        d1.getButton(DialogInterface.BUTTON_NEGATIVE).performClick(); idle()
        assertEquals(5, runBlocking { dao.getAllFlow().first().size })       // Annuler : rien supprimé
        assertTrue(a.top() is TicketDetailFragmentV2)

        a.overlay().findViewById<View>(R.id.btnDeleteTicket).performClick(); idle()
        val d2 = ShadowDialog.getLatestDialog() as AlertDialog
        d2.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        waitFor { runBlocking { dao.getAllFlow().first().size } == 4 }
        idle()
        val left = runBlocking { dao.getAllFlow().first() }
        assertEquals(listOf(1L, 2L, 4L, 5L), left.map { it.id }.sorted())      // seulement CE ticket
        waitFor { a.top() is CategoryDetailFragment }
        assertTrue("Retour au détail de la catégorie après suppression", a.top() is CategoryDetailFragment)
    }

    /** Police 2,0 sur 360 dp : cartes empilées, aucun libellé coupé lettre par lettre (constaté sur émulateur). */
    @Test fun ticket_detail_grande_police_light() {
        RuntimeEnvironment.setFontScale(2.0f)
        seed()
        val a = launch()
        a.openTicketDetail(1L); idle()
        waitFor { a.overlay().findViewById<TextView>(R.id.tvAmount)?.text?.toString() == "64,20 €" }
        assertEquals(android.widget.LinearLayout.VERTICAL, a.overlay().findViewById<android.widget.LinearLayout>(R.id.infoRow).orientation)
        capture(a, "l6_07_ticket_detail_police_2_light")
        a.overlay().findViewById<NestedScrollView>(R.id.ticketDetailScroll).fullScroll(View.FOCUS_DOWN); idle()
        capture(a, "l6_08_ticket_detail_police_2_bas_light")
    }
}
