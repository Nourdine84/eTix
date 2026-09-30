package com.etix.ui.preview

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.BudgetStore
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot 7 — budgets mensuels (iOS CategoryView / CategoryRowView / BudgetSettingsView) : barres correct / attention /
 * dépassé, catégorie sans budget, invitation sans budget, réglage, saisie « 12,50 » ; clair, sombre, grande police.
 * Rendu Robolectric : aperçu, pas une validation visuelle. Vérifie que tickets et catégories restent inchangés.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h780dp-hdpi")
class Lot7ScreenshotTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()

    @Before fun cleanBudgets() { ctx.getSharedPreferences("etix_budgets", Context.MODE_PRIVATE).edit().clear().commit() }

    private fun seed(): List<Ticket> {
        TestDb.reset(ctx)
        val now = System.currentTimeMillis()
        var id = 0L
        fun t(store: String, amount: Double, cat: String) =
            Ticket(id = ++id, store = store, amount = amount, category = cat, dateMillis = now - id * 60_000)
        val list = listOf(
            t("Lidl", 64.20, "Courses"), t("Carrefour", 42.30, "Courses"),   // 106,50 / 120 → attention 89 %
            t("Esso", 58.00, "Transport"),                                   // 58 / 50 → dépassé 116 %
            t("Bistrot", 24.60, "Restaurant"),                               // 24,60 / 80 → correct
            t("Cinéma", 30.00, "Loisirs"),                                   // sans budget
        )
        TestDb.seed(ctx, list)
        return list
    }

    private fun budgets() = BudgetStore(ctx).apply {
        set("Courses", 120.0); set("transport", 50.0); set("Restaurant", 80.0)
    }

    private fun launchCategories(): MainActivityV2 {
        val a = Robolectric.buildActivity(MainActivityV2::class.java).setup().get().also { idle() }
        a.findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.menu_category
        idle()
        waitFor { (a.findViewById<RecyclerView>(R.id.recyclerViewCategories)?.childCount ?: 0) > 1 }
        return a
    }

    private fun MainActivityV2.rowFor(name: String): View? {
        val rv = findViewById<RecyclerView>(R.id.recyclerViewCategories)
        return (0 until rv.childCount).map { rv.getChildAt(it) }
            .firstOrNull { it.findViewById<TextView>(R.id.tvCategoryName)?.text?.toString() == name }
    }

    private fun shootCategories(suffix: String) {
        val seeded = seed(); budgets()
        val a = launchCategories()
        val rv = a.findViewById<RecyclerView>(R.id.recyclerViewCategories)
        rv.scrollToPosition(1); idle()
        val courses = a.rowFor("Courses")!!
        assertEquals(View.VISIBLE, courses.findViewById<View>(R.id.budgetBlock).visibility)
        assertEquals("Attention — 89%", courses.findViewById<TextView>(R.id.tvBudgetStatus).text.toString())
        assertEquals("106,50 € / 120 €", courses.findViewById<TextView>(R.id.tvBudgetAmounts).text.toString())
        capture(a, "l7_01_categories_budgets_$suffix")
        rv.scrollToPosition(rv.adapter!!.itemCount - 1); idle()
        assertEquals("Dépassé — 116%", a.rowFor("Transport")!!.findViewById<TextView>(R.id.tvBudgetStatus).text.toString())
        assertEquals(View.GONE, a.rowFor("Loisirs")!!.findViewById<View>(R.id.budgetBlock).visibility)
        capture(a, "l7_02_categories_budgets_bas_$suffix")

        // Cette année : pas de barre (iOS : budgets sur la vue mensuelle uniquement)
        rv.scrollToPosition(0); idle()
        a.findViewById<View>(R.id.btnCatYear).performClick(); idle()
        rv.scrollToPosition(1); idle()
        waitFor { a.rowFor("Courses")?.findViewById<View>(R.id.budgetBlock)?.visibility == View.GONE }
        assertEquals(View.GONE, a.rowFor("Courses")!!.findViewById<View>(R.id.budgetBlock).visibility)

        // Réglages
        a.findViewById<View>(R.id.btnBudgets).performClick(); idle()
        val overlay = a.findViewById<android.view.ViewGroup>(R.id.overlayContainer)
        waitFor { (overlay.findViewById<android.view.ViewGroup>(R.id.budgetRows)?.childCount ?: 0) > 0 }
        // Fin de l'animation d'entrée (glissement) avant la capture
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS); idle()
        assertTrue(overlay.findViewById<View>(R.id.tvBudgetsTitle).isShown)
        capture(a, "l7_03_reglage_budgets_$suffix")

        assertEquals(seeded.sortedBy { it.id }, runBlocking { dao.getAllFlow().first() }.sortedBy { it.id })
    }

    @Test fun categories_light() = shootCategories("light")

    @Test @Config(qualifiers = "+night")
    fun categories_dark() = shootCategories("dark")

    @Test fun invitation_sans_budget_light() {
        seed()
        val a = launchCategories()
        val rv = a.findViewById<RecyclerView>(R.id.recyclerViewCategories)
        rv.scrollToPosition(rv.adapter!!.itemCount - 1); idle()
        val teaser = a.findViewById<View>(R.id.budgetTeaser)
        assertTrue(teaser != null && teaser.isShown)
        capture(a, "l7_04_invitation_budgets_light")
        teaser.performClick(); idle()
        waitFor { a.findViewById<View>(R.id.tvBudgetsTitle)?.isShown == true }
        assertTrue(a.findViewById<View>(R.id.tvBudgetsTitle).isShown)
    }

    private fun openEdit(a: MainActivityV2, category: String): View {
        a.openBudgetSettings(); idle()
        val rows = a.findViewById<android.view.ViewGroup>(R.id.budgetRows)
        waitFor { rows.childCount > 0 }
        val row = (0 until rows.childCount).map { rows.getChildAt(it) }
            .first { it.findViewById<TextView>(R.id.tvBudgetCategory)?.text?.toString() == category }
        row.performClick(); idle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS); idle()
        return a.findViewById<android.view.ViewGroup>(R.id.overlayContainer)
    }

    @Test fun saisie_francaise_appliquer_modifier_supprimer() {
        seed()
        val a = launchCategories()
        val o = openEdit(a, "Loisirs")
        val input = o.findViewById<EditText>(R.id.inputBudget)
        val apply = o.findViewById<View>(R.id.btnBudgetApply)
        assertFalse("Appliquer inactif sans montant", apply.isEnabled)
        input.setText("abc"); idle(); assertFalse(apply.isEnabled)
        input.setText("0"); idle(); assertFalse(apply.isEnabled)
        input.setText("12,50"); idle(); assertTrue(apply.isEnabled)
        capture(a, "l7_05_saisie_budget_light")
        apply.performClick(); idle()
        assertEquals(12.5, BudgetStore(ctx).limit("Loisirs")!!, 0.0)
        waitFor { a.findViewById<android.view.ViewGroup>(R.id.budgetRows)?.let { r ->
            (0 until r.childCount).any { (r.getChildAt(it).findViewById<TextView>(R.id.tvBudgetValue))?.text?.toString() == "12,50 €" } } == true }

        // Modification : champ prérempli « 12,50 » (iOS : « 13 », voir docs/BUDGETS.md A1)
        a.onBackPressedDispatcher.onBackPressed(); idle()
        val o2 = openEdit(a, "Loisirs")
        assertEquals("12,50", o2.findViewById<EditText>(R.id.inputBudget).text.toString())
        val del = o2.findViewById<View>(R.id.btnDeleteBudget)
        assertEquals(View.VISIBLE, del.visibility)
        BudgetStore(ctx).set("Transport", 50.0) // autre budget : ne doit pas être touché
        val ticketsAvant = runBlocking { dao.getAllFlow().first() }.sortedBy { it.id }

        // 1) Confirmation demandée ; « Annuler » ne modifie rien
        del.performClick(); idle()
        val c1 = org.robolectric.shadows.ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
        assertTrue(c1.isShowing)
        c1.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick(); idle()
        assertEquals(12.5, BudgetStore(ctx).limit("Loisirs")!!, 0.0)
        assertTrue("Toujours sur l'écran de saisie", o2.findViewById<View>(R.id.btnBudgetApply).isShown)

        // 2) « Supprimer » : seul le budget « Loisirs » disparaît
        del.performClick(); idle()
        val c2 = org.robolectric.shadows.ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
        c2.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick(); idle()
        assertEquals(null, BudgetStore(ctx).limit("Loisirs"))
        assertEquals(50.0, BudgetStore(ctx).limit("Transport")!!, 0.0)
        assertEquals(ticketsAvant, runBlocking { dao.getAllFlow().first() }.sortedBy { it.id }) // aucun ticket touché
    }

    /** Budget partagé par deux catégories ne différant que par la casse : consommation cumulée, lignes distinctes. */
    private fun sharedCase(suffix: String) {
        TestDb.reset(ctx)
        val now = System.currentTimeMillis()
        val seeded = listOf(
            Ticket(id = 1, store = "Lidl", amount = 30.0, category = "Courses", dateMillis = now - 60_000),
            Ticket(id = 2, store = "Marché", amount = 20.0, category = "courses", dateMillis = now - 120_000),
        )
        TestDb.seed(ctx, seeded)
        BudgetStore(ctx).set("Courses", 40.0)
        val a = launchCategories()
        a.findViewById<RecyclerView>(R.id.recyclerViewCategories).scrollToPosition(2); idle()
        for ((name, own, other) in listOf(Triple("Courses", "30,00 €", "courses"), Triple("courses", "20,00 €", "Courses"))) {
            val row = a.rowFor(name)!!
            assertEquals(own, row.findViewById<TextView>(R.id.tvCategoryTotal).text.toString())          // total individuel
            assertEquals("Dépassé — 125%", row.findViewById<TextView>(R.id.tvBudgetStatus).text.toString())
            assertEquals("50 € / 40 €", row.findViewById<TextView>(R.id.tvBudgetAmounts).text.toString())
            val shared = row.findViewById<TextView>(R.id.tvBudgetShared)
            assertEquals(View.VISIBLE, shared.visibility)
            assertEquals("Budget partagé avec « $other » · consommation cumulée", shared.text.toString())
        }
        capture(a, "l7_11_budget_partage_casse_$suffix")
        assertEquals(seeded, runBlocking { dao.getAllFlow().first() }.sortedBy { it.id })          // tickets intacts
    }

    @Test fun budget_partage_casse_light() = sharedCase("light")

    @Test @Config(qualifiers = "+night")
    fun budget_partage_casse_dark() = sharedCase("dark")

    /** Écart A1 : valider sans retoucher conserve 12,50 € (iOS enregistrerait 13 €). */
    @Test fun valider_sans_modifier_conserve_le_montant_exact() {
        seed()
        BudgetStore(ctx).set("Loisirs", 12.5)
        val a = launchCategories()
        val o = openEdit(a, "Loisirs")
        assertEquals("12,50", o.findViewById<EditText>(R.id.inputBudget).text.toString())
        o.findViewById<View>(R.id.btnBudgetApply).performClick(); idle()
        assertEquals(12.5, BudgetStore(ctx).limit("Loisirs")!!, 0.0)
    }

    @Test fun annuler_ne_modifie_rien() {
        seed(); budgets()
        val a = launchCategories()
        val o = openEdit(a, "Courses")
        o.findViewById<EditText>(R.id.inputBudget).setText("999"); idle()
        o.findViewById<View>(R.id.btnBudgetCancel).performClick(); idle()
        assertEquals(120.0, BudgetStore(ctx).limit("Courses")!!, 0.0)
    }

    @Test @Config(qualifiers = "w320dp-h640dp-hdpi")
    fun petit_ecran_grande_police_light() {
        RuntimeEnvironment.setFontScale(1.3f)
        seed(); budgets()
        val a = launchCategories()
        a.findViewById<RecyclerView>(R.id.recyclerViewCategories).scrollToPosition(2); idle()
        capture(a, "l7_06_categories_budgets_320dp_police_1_3_light")
        openEdit(a, "Courses")
        capture(a, "l7_07_saisie_budget_320dp_police_1_3_light")
    }

    @Test fun police_2_light() {
        RuntimeEnvironment.setFontScale(2.0f)
        seed(); budgets()
        val a = launchCategories()
        a.findViewById<RecyclerView>(R.id.recyclerViewCategories).scrollToPosition(2); idle()
        capture(a, "l7_08_categories_budgets_police_2_light")
        openEdit(a, "Courses")
        capture(a, "l7_09_saisie_budget_police_2_light")
    }

}
