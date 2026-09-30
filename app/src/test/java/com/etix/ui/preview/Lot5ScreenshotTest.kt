package com.etix.ui.preview

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import com.etix.testutil.Screens.capture
import com.etix.testutil.Screens.idle
import com.etix.testutil.Screens.waitFor
import com.etix.testutil.TestDb
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
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Lot 5 — onglet Catégories (iOS CategoryView) : anneau, légende, lignes avec variation, état vide ; clair et sombre.
 * Rendu Robolectric : aperçu, pas une validation visuelle. Vérifie aussi que l'écran ne modifie aucun ticket.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h780dp-hdpi")
class Lot5ScreenshotTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private val day = TimeUnit.DAYS.toMillis(1)

    private fun seed(empty: Boolean = false): List<Ticket> {
        TestDb.reset(ctx)
        if (empty) return emptyList()
        val now = System.currentTimeMillis()
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        var id = 0L
        fun t(store: String, amount: Double, at: Long, cat: String) =
            Ticket(id = ++id, store = store, amount = amount, category = cat, dateMillis = at)
        val list = listOf(
            // Mois courant (aujourd'hui)
            t("Lidl", 64.20, now - 60_000, "Courses"),
            t("Carrefour", 42.30, now - 2 * 60_000, "Courses"),
            t("Esso", 58.00, now - 3 * 60_000, "Transport"),
            t("Le Petit Bistrot", 24.60, now - 4 * 60_000, "Restaurant"),
            t("Cinéma", 30.00, now - 5 * 60_000, "Loisirs"),
            t("Pharmacie", 13.50, now - 6 * 60_000, "Santé"),
            t("Marché", 12.00, now - 7 * 60_000, "Autre"),
            t("Fnac", 9.90, now - 8 * 60_000, "Cadeaux perso"), // catégorie libre existante : conservée telle quelle
            // Mois précédent → variations
            t("Lidl", 90.00, monthStart - 5 * day, "Courses"),
            t("Brasserie", 40.00, monthStart - 6 * day, "Restaurant"),
            t("Esso", 58.00, monthStart - 7 * day, "Transport"),
            // Plus ancien (année)
            t("Décathlon", 120.00, monthStart - 70 * day, "Loisirs"),
        )
        TestDb.seed(ctx, list)
        return list
    }

    private fun expected(list: List<Ticket>, r: TimeRange) = String.format(java.util.Locale.FRANCE, "%.2f €",
        list.filter { it.dateMillis in r.currentRange() }.sumOf { it.amount })

    private fun launchOnCategories(): MainActivityV2 {
        val a = Robolectric.buildActivity(MainActivityV2::class.java).setup().get().also { idle() }
        a.findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.menu_category
        idle()
        return a
    }

    private fun MainActivityV2.rows() = findViewById<RecyclerView>(R.id.recyclerViewCategories)

    private fun MainActivityV2.period(id: Int) { findViewById<View>(id).performClick(); idle() }

    private fun shoot(suffix: String) {
        val seeded = seed()
        val a = launchOnCategories()
        waitFor { (a.rows().childCount) > 1 }
        val donutTotal = a.findViewById<TextView>(R.id.tvDonutTotal).text.toString()
        assertEquals(expected(seeded, TimeRange.MONTH), donutTotal)
        capture(a, "l5_01_categories_mois_$suffix")

        a.rows().scrollToPosition(a.rows().adapter!!.itemCount - 1); idle()
        capture(a, "l5_02_categories_mois_bas_$suffix")

        a.rows().scrollToPosition(0); idle() // la carte anneau (position 0) doit être attachée
        a.period(R.id.btnCatYear)
        waitFor { a.findViewById<TextView>(R.id.tvDonutTotal)?.text?.toString() == expected(seeded, TimeRange.YEAR) }
        assertEquals(expected(seeded, TimeRange.YEAR), a.findViewById<TextView>(R.id.tvDonutTotal).text.toString())
        capture(a, "l5_03_categories_annee_$suffix")

        // Aucun ticket modifié ou supprimé par l'écran
        val after = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }
        assertEquals(seeded.size, after.size)
        assertEquals(seeded.map { it.category }.sorted(), after.map { it.category }.sorted())
    }

    private fun shootEmpty(suffix: String) {
        seed(empty = true)
        val a = launchOnCategories()
        waitFor { a.findViewById<View>(R.id.emptyCategories).visibility == View.VISIBLE }
        assertTrue(a.findViewById<View>(R.id.emptyCategories).visibility == View.VISIBLE)
        capture(a, "l5_04_categories_vide_$suffix")
    }

    @Test fun categories_light() = shoot("light")

    @Test @Config(qualifiers = "+night")
    fun categories_dark() = shoot("dark")

    @Test fun categories_empty_light() = shootEmpty("light")

    @Test @Config(qualifiers = "+night")
    fun categories_empty_dark() = shootEmpty("dark")
}
