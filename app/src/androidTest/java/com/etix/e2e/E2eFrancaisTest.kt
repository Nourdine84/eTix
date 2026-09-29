package com.etix.e2e

import android.content.Intent
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.pickerText
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Émulateur configuré en français (fr-FR), données FICTIVES, app neuve.
 * Couvre : « 12,50 » tapé au clavier FR, affichage des montants, dates jour/mois, sélecteur de date en saisie
 * texte (jj/mm/aaaa), bornes INCLUSIVES du filtre de l'Historique, limites des périodes Aujourd'hui / Ce mois.
 * Données de bornes injectées directement en base (horodatages exacts à la milliseconde).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eFrancaisTest {

    private val todayStart: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val monthStart: Long = Calendar.getInstance().apply {
        timeInMillis = todayStart; set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
    private fun dayOffset(days: Int): Long =
        Calendar.getInstance().apply { timeInMillis = todayStart; add(Calendar.DAY_OF_MONTH, days) }.timeInMillis
    private val mediumFr get() = DateFormat.getDateInstance(DateFormat.MEDIUM)

    @Before
    fun localeEtSession() {
        assertEquals("Émulateur attendu en français", "fr", Locale.getDefault().language)
        SessionManager(ctx).apply { markFirstLaunchDone(); login("Testeur FR") }
    }

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor(withId(R.id.bottomNav))
    }

    private fun seed(vararg t: Ticket) = runBlocking {
        val dao = AppDatabase.getInstance(ctx).ticketDao()
        t.forEach { dao.insert(it) }
    }

    private fun typeInPicker(c: Calendar) {
        waitFor(withId(com.google.android.material.R.id.mtrl_picker_header_toggle))
        onView(withId(com.google.android.material.R.id.mtrl_picker_header_toggle)).perform(click())
        onView(allOf(isAssignableFrom(EditText::class.java),
            isDescendantOfA(withId(com.google.android.material.R.id.mtrl_picker_text_input_date))))
            .perform(replaceText(pickerText(c)))
        closeSoftKeyboard()
    }

    private fun confirmPicker() =
        onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())

    /** Présence dans toute la liste (y compris hors écran) : scrollTo parcourt l'adaptateur. */
    private fun inStores(name: String) =
        onView(withId(R.id.recyclerStores)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(name))))

    private fun notInStores(name: String) {
        try {
            inStores(name)
            fail("« $name » ne devrait pas figurer dans la liste")
        } catch (expected: PerformException) { /* absent de tout l'adaptateur */ }
    }

    @Test
    fun f01_saisie_virgule_clavier_fr_et_affichage() {
        startMain()
        onView(withId(R.id.menu_add)).perform(click())
        onView(allOf(withId(R.id.inputStore), isDisplayed())).perform(click(), typeText("Fr Virgule"))
        closeSoftKeyboard()
        onView(withId(R.id.inputAmount)).perform(scrollTo(), click(), typeText("12,50"))
        shot("f01_ajout_virgule_fr")
        onView(withId(R.id.inputAmount)).check(matches(withText("12,50")))
        // Date du jour au format français (ex. « 29 sept. 2026 »)
        val today = mediumFr.format(java.util.Date())
        onView(withId(R.id.inputDate)).check(matches(withText(today)))
        assertTrue("Format de date non français : $today", Regex("^\\d{1,2} \\p{L}+\\.? \\d{4}$").matches(today))
        closeSoftKeyboard()
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())

        onView(withId(R.id.menu_history)).perform(click())
        onView(withId(R.id.inputSearch)).perform(click(), typeText("Fr Virgule"))
        closeSoftKeyboard()
        waitFor(allOf(withId(R.id.tvAmount), withText("12,50 €")))
        waitFor(allOf(withId(R.id.tvDate), withText(containsString(today))))
        shot("f02_historique_montant_fr")
        onView(withId(R.id.inputSearch)).perform(replaceText(""))
    }

    @Test
    fun f02_selecteur_date_jour_mois() {
        // Jour > 12 pour lever toute ambiguïté jour/mois ; date passée
        val target = Calendar.getInstance().apply {
            if (get(Calendar.DAY_OF_MONTH) < 15) add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val typed = pickerText(target)
        assertTrue("Saisie attendue jj/mm/aaaa, obtenu $typed", typed.startsWith("15/"))

        startMain()
        onView(withId(R.id.menu_add)).perform(click())
        onView(allOf(withId(R.id.inputStore), isDisplayed())).perform(click(), typeText("Fr Date"))
        closeSoftKeyboard()
        onView(withId(R.id.inputAmount)).perform(scrollTo(), click(), typeText("3,40"))
        closeSoftKeyboard()
        onView(withId(R.id.inputDate)).perform(scrollTo(), click())
        typeInPicker(target)
        shot("f03_selecteur_date_fr")
        confirmPicker()
        val expected = mediumFr.format(target.time)
        waitFor(allOf(withId(R.id.inputDate), withText(expected)))
        shot("f04_date_jour_mois_fr")
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())

        onView(withId(R.id.menu_history)).perform(click())
        onView(withId(R.id.inputSearch)).perform(click(), typeText("Fr Date"))
        closeSoftKeyboard()
        waitFor(allOf(withId(R.id.tvDate), withText(containsString(expected))))
        onView(withId(R.id.inputSearch)).perform(replaceText(""))
    }

    @Test
    fun f03_filtre_historique_bornes_inclusives() {
        val d = todayStart
        val dMinus1 = dayOffset(-1)
        seed(
            Ticket(store = "Fr Limite A", amount = 1.0, category = "Autre", dateMillis = dMinus1 - 1),          // J-2 23:59:59.999
            Ticket(store = "Fr Limite B", amount = 2.0, category = "Autre", dateMillis = dMinus1),              // J-1 00:00:00.000
            Ticket(store = "Fr Limite C", amount = 3.0, category = "Autre", dateMillis = d - 1),                // J-1 23:59:59.999
            Ticket(store = "Fr Limite D", amount = 4.0, category = "Autre", dateMillis = d),                    // J   00:00:00.000
        )
        val jMoins1 = Calendar.getInstance().apply { timeInMillis = dMinus1 }

        startMain()
        onView(withId(R.id.menu_history)).perform(click())
        onView(withId(R.id.inputSearch)).perform(click(), typeText("Fr Limite"))
        closeSoftKeyboard()
        waitFor(withText("Fr Limite A"))

        // Filtre du J-1 au J-1 : début ET fin inclus → B et C seulement
        onView(withId(R.id.btnFilter)).perform(click())
        onView(withId(R.id.switchStart)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.tvStart)).inRoot(isDialog()).perform(click())
        typeInPicker(jMoins1); confirmPicker()
        onView(withId(R.id.switchEnd)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.tvEnd)).inRoot(isDialog()).perform(click())
        typeInPicker(jMoins1); confirmPicker()
        shot("f05_filtre_inclusif_fr")
        onView(withText("Appliquer")).inRoot(isDialog()).perform(click())

        waitFor(withText("Fr Limite B"))
        waitFor(withText("Fr Limite C"))
        // Portée : liste de l'Historique (la page Magasins, hors écran, contient aussi ces noms)
        onView(allOf(withText("Fr Limite A"), isDescendantOfA(withId(R.id.recyclerHistory)))).check(doesNotExist())
        onView(allOf(withText("Fr Limite D"), isDescendantOfA(withId(R.id.recyclerHistory)))).check(doesNotExist())
        waitFor(allOf(withId(R.id.tvFilterSummary), withText(containsString(mediumFr.format(jMoins1.time)))))
        shot("f06_historique_filtre_inclusif_fr")

        onView(withId(R.id.btnFilter)).perform(click())
        onView(withText("Réinitialiser")).inRoot(isDialog()).perform(click())
        onView(withId(R.id.inputSearch)).perform(replaceText(""))
    }

    @Test
    fun f04_limites_des_periodes_magasins() {
        seed(
            Ticket(store = "Fr Hier Fin", amount = 5.0, category = "Autre", dateMillis = todayStart - 1),
            Ticket(store = "Fr Aujourdhui Debut", amount = 6.0, category = "Autre", dateMillis = todayStart),
            Ticket(store = "Fr Mois Precedent Fin", amount = 7.0, category = "Autre", dateMillis = monthStart - 1),
            Ticket(store = "Fr Mois Debut", amount = 8.0, category = "Autre", dateMillis = monthStart),
        )
        startMain()
        onView(withId(R.id.menu_stores)).perform(click())

        onView(withId(R.id.btnPeriodToday)).perform(click())
        waitFor(withText("Fr Aujourdhui Debut"))
        inStores("Fr Aujourdhui Debut")
        notInStores("Fr Hier Fin")
        shot("f07_magasins_aujourdhui_limites_fr")

        onView(withId(R.id.btnPeriodMonth)).perform(click())
        inStores("Fr Mois Debut")
        notInStores("Fr Mois Precedent Fin")
        inStores("Fr Mois Debut")
        shot("f08_magasins_mois_limites_fr")
    }
}
