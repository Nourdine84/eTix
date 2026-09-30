package com.etix.ui.preview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.BudgetStore
import com.etix.model.Ticket
import com.etix.testutil.Screens.capture
import com.etix.testutil.Screens.idle
import com.etix.testutil.Screens.waitFor
import com.etix.testutil.TestDb
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot 8 — carte Budget de l'Accueil (iOS BudgetSummaryCardView) et effet « budget tendu » sur la phrase.
 * Rendu Robolectric : aperçu, pas une validation visuelle. Tickets, catégories et budgets jamais modifiés.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h780dp-hdpi")
class Lot8ScreenshotTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()

    @Before fun clean() { ctx.getSharedPreferences("etix_budgets", Context.MODE_PRIVATE).edit().clear().commit() }

    /** 3 tickets ce mois (hors « Loisirs » : 10 €), 2 le mois précédent pour une comparaison stable (+1 %). */
    private fun seed(courses: Double, coursesMaj: Double = 0.0): List<Ticket> {
        TestDb.reset(ctx)
        val now = System.currentTimeMillis()
        val prevMonth = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1); set(java.util.Calendar.DAY_OF_MONTH, 10) }.timeInMillis
        val list = mutableListOf(
            Ticket(id = 1, store = "Lidl", amount = courses, category = "Courses", dateMillis = now - 60_000),
            Ticket(id = 2, store = "Cinéma", amount = 10.0, category = "Loisirs", dateMillis = now - 120_000),
            Ticket(id = 3, store = "Esso", amount = 5.0, category = "Transport", dateMillis = now - 180_000),
            Ticket(id = 4, store = "Ancien", amount = courses + coursesMaj + 15.0 - 0.5, category = "Courses", dateMillis = prevMonth),
            Ticket(id = 5, store = "Ancien2", amount = 0.5, category = "Transport", dateMillis = prevMonth),
        )
        if (coursesMaj > 0) list += Ticket(id = 6, store = "Marché", amount = coursesMaj, category = "courses", dateMillis = now - 240_000)
        TestDb.seed(ctx, list)
        return list
    }

    private fun launch(): MainActivityV2 = Robolectric.buildActivity(MainActivityV2::class.java).setup().get().also {
        idle(); waitFor { it.findViewById<TextView>(R.id.tvTicketCount)?.text?.isNotEmpty() == true }
    }

    private fun MainActivityV2.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun scrollToBudget(a: MainActivityV2) {
        val card = a.findViewById<View>(R.id.cardBudget)
        a.findViewById<NestedScrollView>(R.id.homeScroll).scrollTo(0, (card.top - 40).coerceAtLeast(0)); idle()
    }

    private fun case(name: String, courses: Double, limit: Double, headline: String, percent: String,
                     narration: String, suffix: String = "light", coursesMaj: Double = 0.0) {
        val seeded = seed(courses, coursesMaj)
        BudgetStore(ctx).apply { set("Courses", limit); set("Loisirs", 100.0) }
        val a = launch()
        waitFor { a.findViewById<View>(R.id.cardBudget).visibility == View.VISIBLE }
        assertEquals(headline, a.text(R.id.tvBudgetHeadline))
        assertEquals(percent, a.text(R.id.tvBudgetPercent))
        assertEquals(narration, a.text(R.id.tvNarration))
        capture(a, "l8_${name}_haut_$suffix")
        scrollToBudget(a)
        capture(a, "l8_${name}_$suffix")
        assertEquals(seeded.sortedBy { it.id }, runBlocking { dao.getAllFlow().first() }.sortedBy { it.id })
        assertEquals(limit, BudgetStore(ctx).limit("Courses")!!, 0.0)
    }

    // Budgets totaux 100 (Courses) + 100 (Loisirs) = 200 ; Loisirs 10 € dépensés.
    @Test fun sous_le_seuil_light() =
        case("01_confort", courses = 60.0, limit = 100.0, headline = "Il te reste 130\u00A0€", percent = "35%",
            narration = "Tes dépenses sont stables")                        // 70 / 200
    @Test fun attention_light() =
        case("02_attention", courses = 100.0, limit = 100.0, headline = "Il te reste 90\u00A0€", percent = "55%",
            narration = "Tes dépenses sont stables")                        // 110 / 200
    @Test fun au_seuil_critique_light() =
        case("03_critique", courses = 150.0, limit = 100.0, headline = "Il te reste 40\u00A0€", percent = "80%",
            narration = "Ton rythme de dépenses augmente")                  // 160 / 200 → budget tendu
    @Test fun depassement_light() =
        case("04_depasse", courses = 230.0, limit = 100.0, headline = "Budgets dépassés de 40\u00A0€", percent = "120%",
            narration = "Ton rythme de dépenses augmente")                  // 240 / 200
    @Test @Config(qualifiers = "+night")
    fun depassement_dark() =
        case("04_depasse", courses = 230.0, limit = 100.0, headline = "Budgets dépassés de 40\u00A0€", percent = "120%",
            narration = "Ton rythme de dépenses augmente", suffix = "dark")
    @Test @Config(qualifiers = "+night")
    fun attention_dark() =
        case("02_attention", courses = 100.0, limit = 100.0, headline = "Il te reste 90\u00A0€", percent = "55%",
            narration = "Tes dépenses sont stables", suffix = "dark")

    /** Budget partagé « Courses » / « courses » : compté une fois, dépenses cumulées 30 + 20 = 50 (+ Loisirs 10). */
    @Test fun budget_partage_light() {
        case("05_partage", courses = 30.0, limit = 40.0, headline = "Il te reste 80\u00A0€", percent = "42%",
            narration = "Tes dépenses sont stables", coursesMaj = 20.0)     // 60 / 140
        val a = Robolectric.buildActivity(MainActivityV2::class.java).setup().get().also { idle() }
        waitFor { a.findViewById<ViewGroup>(R.id.budgetLines)?.childCount == 2 }
        val first = a.findViewById<ViewGroup>(R.id.budgetLines).getChildAt(0)
        assertEquals("Courses", first.findViewById<TextView>(R.id.tvLineName).text.toString())
        assertEquals("125%", first.findViewById<TextView>(R.id.tvLinePercent).text.toString())
        assertEquals("60\u00A0€ dépensés sur 140\u00A0€ prévus", a.text(R.id.tvBudgetCaption))
    }

    @Test fun sans_budget_pas_de_carte_light() {
        seed(60.0)
        val a = launch()
        assertEquals(View.GONE, a.findViewById<View>(R.id.cardBudget).visibility)
        capture(a, "l8_06_sans_budget_light")
    }

    @Test fun cette_annee_pas_de_carte_ni_budget_tendu() {
        seed(230.0)
        BudgetStore(ctx).set("Courses", 100.0)
        val a = launch()
        a.findViewById<View>(R.id.btnHomeYear).performClick(); idle()
        waitFor { a.findViewById<View>(R.id.cardBudget).visibility == View.GONE }
        assertEquals(View.GONE, a.findViewById<View>(R.id.cardBudget).visibility)
    }

    @Test @Config(qualifiers = "w320dp-h640dp-hdpi")
    fun petit_ecran_police_1_3_light() {
        RuntimeEnvironment.setFontScale(1.3f)
        case("07_320dp_police_1_3", courses = 230.0, limit = 100.0, headline = "Budgets dépassés de 40\u00A0€", percent = "120%",
            narration = "Ton rythme de dépenses augmente")
    }

    @Test fun police_2_light() {
        RuntimeEnvironment.setFontScale(2.0f)
        case("08_police_2", courses = 150.0, limit = 100.0, headline = "Il te reste 40\u00A0€", percent = "80%",
            narration = "Ton rythme de dépenses augmente")
    }
}
