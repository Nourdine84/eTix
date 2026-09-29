package com.etix.ui.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.model.Ticket
import com.etix.testutil.TestDb
import com.etix.ui.main.MainActivityV2
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Aperçu visuel du lot 2 (rendu natif Robolectric, sans émulateur).
 * Les PNG sont écrits dans app/build/screenshots/ et publiés par la CI.
 * Ce n'est PAS une validation visuelle : le rendu Robolectric peut différer d'un appareil
 * (polices, ombres, insets système absents).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h780dp-hdpi")
class Lot2ScreenshotTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val now = System.currentTimeMillis()

    @Before
    fun seed() {
        val ctx = RuntimeEnvironment.getApplication()
        TestDb.reset(ctx)
        fun t(id: Long, store: String, amount: Double, daysAgo: Int, cat: String) =
            Ticket(id = id, store = store, amount = amount, category = cat, dateMillis = now - daysAgo * day)
        TestDb.seed(ctx, listOf(
            t(1, "Lidl", 42.30, 1, "Courses"),
            t(2, "Lidl", 18.75, 4, "Courses"),
            t(3, "LIDL", 9.90, 9, "Maison"),
            t(4, "Lidl", 55.10, 35, "Courses"),
            t(5, "Lidl", 12.00, 70, "Hygiène"),
            t(6, "Lidl", 23.40, 12, "Courses"),
            t(7, "Carrefour", 64.20, 3, "Courses"),
            t(8, "Carrefour", 21.00, 15, "Maison"),
            t(9, "Esso", 58.00, 6, "Transport"),
            t(10, "Boulangerie Paul", 4.60, 2, "Restaurant"),
            t(11, "Boulangerie Paul", 7.80, 8, "Restaurant"),
            t(12, "Pharmacie du Centre", 13.50, 20, "Santé"),
        ))
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Room émet sur un thread d'arrière-plan : on laisse tourner le looper jusqu'à la condition. */
    private fun waitFor(cond: () -> Boolean) {
        repeat(100) {
            idle()
            if (cond()) return
            Thread.sleep(50)
        }
        idle()
    }

    private fun launch(): MainActivityV2 =
        Robolectric.buildActivity(MainActivityV2::class.java).setup().get().also { idle() }

    private fun capture(activity: MainActivityV2, name: String) {
        idle()
        val root = activity.window.decorView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun MainActivityV2.openStoresTab() {
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.menu_stores
        waitFor { (findViewById<RecyclerView>(R.id.recyclerStores)?.childCount ?: 0) > 0 }
    }

    @Test
    fun home_with_settings_entry() {
        capture(launch(), "01_accueil_reglages_light")
    }

    @Test
    fun stores_month_light() {
        val a = launch(); a.openStoresTab()
        capture(a, "02_magasins_mois_light")
    }

    @Test
    @Config(qualifiers = "+night")
    fun stores_month_dark() {
        val a = launch(); a.openStoresTab()
        capture(a, "03_magasins_mois_dark")
    }

    @Test
    fun stores_empty_today() {
        val a = launch(); a.openStoresTab()
        a.findViewById<View>(R.id.btnPeriodToday).performClick()
        waitFor { a.findViewById<View>(R.id.emptyStores).visibility == View.VISIBLE }
        capture(a, "04_magasins_vide_aujourdhui_light")
    }

    @Test
    fun store_detail_light() {
        val a = launch(); a.openStoresTab()
        a.openStoreDetail("lidl")
        waitFor { a.findViewById<android.widget.TextView>(R.id.tvDetailTotal)?.text?.isNotEmpty() == true }
        capture(a, "05_fiche_magasin_light")
        a.findViewById<androidx.core.widget.NestedScrollView>(R.id.storeDetailScroll).fullScroll(View.FOCUS_DOWN)
        capture(a, "05b_fiche_magasin_bas_light")
    }

    @Test
    @Config(qualifiers = "+night")
    fun store_detail_dark() {
        val a = launch(); a.openStoresTab()
        a.openStoreDetail("lidl")
        waitFor { a.findViewById<android.widget.TextView>(R.id.tvDetailTotal)?.text?.isNotEmpty() == true }
        capture(a, "06_fiche_magasin_dark")
    }

    @Test
    fun settings_overlay() {
        val a = launch()
        a.findViewById<View>(R.id.btnSettings).performClick()
        idle()
        capture(a, "07_reglages_depuis_accueil_light")
    }
}
