package com.etix.ui.preview

import android.view.View
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.Ticket
import com.etix.testutil.Screens
import com.etix.testutil.Screens.capture
import com.etix.testutil.Screens.idle
import com.etix.testutil.Screens.waitFor
import com.etix.testutil.TestDb
import com.etix.ui.main.MainActivityV2
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Aperçus du lot 3 : Accueil aligné iOS + thème sombre sur tous les écrans principaux.
 * Chaque écran en clair ET en sombre. Rendu Robolectric : pas une validation visuelle.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h780dp-hdpi")
class Lot3ScreenshotTest {

    private val day = TimeUnit.DAYS.toMillis(1)

    /** Données : mois courant 3 tickets, mois précédent 2, et 4 mois antérieurs → tendance remplie. */
    private fun seed(empty: Boolean = false) {
        val ctx = RuntimeEnvironment.getApplication()
        TestDb.reset(ctx)
        if (empty) return
        val now = System.currentTimeMillis()
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        fun t(id: Long, store: String, amount: Double, at: Long, cat: String) =
            Ticket(id = id, store = store, amount = amount, category = cat, dateMillis = at)
        TestDb.seed(ctx, listOf(
            t(1, "Lidl", 42.30, now - 60_000, "Courses"),
            t(2, "Carrefour", 64.20, now - 2 * 60_000, "Courses"),
            t(3, "Boulangerie Paul", 4.60, now - 3 * 60_000, "Restaurant"),
            t(4, "Esso", 58.00, monthStart - 5 * day, "Transport"),
            t(5, "Lidl", 31.90, monthStart - 12 * day, "Courses"),
            t(6, "Pharmacie du Centre", 13.50, monthStart - 40 * day, "Santé"),
            t(7, "Carrefour", 88.00, monthStart - 70 * day, "Courses"),
            t(8, "Lidl", 51.20, monthStart - 100 * day, "Courses"),
            t(9, "Esso", 60.00, monthStart - 130 * day, "Transport"),
        ))
    }

    private fun launch(): MainActivityV2 =
        Robolectric.buildActivity(MainActivityV2::class.java).setup().get().also { idle() }

    private fun MainActivityV2.tab(id: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = id
        idle()
    }

    private fun MainActivityV2.homeLoaded() =
        waitFor { findViewById<TextView>(R.id.tvTicketCount)?.text?.isNotEmpty() == true }

    private fun shoot(suffix: String) {
        seed()
        val a = launch()
        a.homeLoaded()
        capture(a, "l3_01_accueil_$suffix")

        a.findViewById<NestedScrollView>(R.id.homeScroll).fullScroll(View.FOCUS_DOWN)
        capture(a, "l3_02_accueil_bas_$suffix")

        a.tab(R.id.menu_add)
        capture(a, "l3_03_ajouter_$suffix")

        a.tab(R.id.menu_history)
        waitFor { (a.findViewById<RecyclerView>(R.id.recyclerHistory)?.childCount ?: 0) > 0 }
        capture(a, "l3_04_historique_$suffix")

        a.tab(R.id.menu_category)
        waitFor { (a.findViewById<RecyclerView>(R.id.recyclerViewCategories)?.childCount ?: 0) > 0 }
        capture(a, "l3_05_categories_$suffix")

        a.tab(R.id.menu_home)
        a.openTicketDetail(1L)
        val overlay = a.findViewById<android.view.ViewGroup>(R.id.overlayContainer)
        waitFor { overlay.findViewById<TextView>(R.id.tvAmount)?.text?.isNotEmpty() == true }
        capture(a, "l3_06_detail_ticket_$suffix")

        overlay.findViewById<View>(R.id.btnEdit).performClick()
        waitFor { overlay.findViewById<TextView>(R.id.inputStore)?.text?.isNotEmpty() == true }
        capture(a, "l3_07_edition_ticket_$suffix")

        a.goToPage(MainActivityV2.PAGE_HOME)
        a.openSettings()
        capture(a, "l3_08_reglages_$suffix")
    }

    @Test
    fun screens_light() = shoot("light")

    @Test
    @Config(qualifiers = "+night")
    fun screens_dark() = shoot("dark")

    @Test
    fun home_empty_light() {
        seed(empty = true)
        val a = launch(); a.homeLoaded()
        capture(a, "l3_09_accueil_vide_light")
    }

    @Test
    @Config(qualifiers = "+night")
    fun home_empty_dark() {
        seed(empty = true)
        val a = launch(); a.homeLoaded()
        capture(a, "l3_09_accueil_vide_dark")
    }
}
