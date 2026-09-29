package com.etix.e2e

import android.content.Intent
import android.widget.EditText
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.R
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.ui.main.MainActivityV2
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasToString
import org.hamcrest.Matchers.not
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Lot 4 sur émulateur (données fictives) : date et catégorie à l'ajout, sections et filtres de l'Historique,
 * recherche, modification de la date et d'une catégorie libre. Aucune suppression dans ce parcours.
 * Prérequis : session ouverte (E2eParcoursTest déjà exécuté).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eLot4Test {

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor(withId(R.id.bottomNav))
    }

    private fun shown(id: Int) = allOf(withId(id), isDisplayed())
    private fun inOverlay(id: Int) = allOf(withId(id), isDescendantOfA(withId(R.id.overlayContainer)))

    /** Jour ciblé : le 1er du mois précédent (section « Plus ancien », comparaison mensuelle). */
    private val target: Calendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 1)
    }

    /** Format de saisie texte de MaterialDatePicker (UtcDates.getDefaultTextInputFormat). */
    private fun pickerText(c: Calendar): String {
        val base = (DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat).toPattern()
        val pattern = base.replace(Regex("\\s+"), "").replace(Regex("d{1,2}"), "dd")
            .replace(Regex("M{1,2}"), "MM").replace(Regex("y{1,4}"), "yyyy")
        return SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear(); set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
            }.time)
    }

    private fun typeDateInPicker(c: Calendar) {
        onView(withId(com.google.android.material.R.id.mtrl_picker_header_toggle)).perform(click())
        onView(allOf(isAssignableFrom(EditText::class.java),
            isDescendantOfA(withId(com.google.android.material.R.id.mtrl_picker_text_input_date))))
            .perform(replaceText(pickerText(c)))
        closeSoftKeyboard()
        onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
    }

    @Test
    fun c01_ajout_avec_date_categorie_description() {
        startMain()
        onView(withId(R.id.menu_add)).perform(click())
        onView(shown(R.id.inputStore)).perform(click(), typeText("Esso Test"))
        closeSoftKeyboard()
        onView(withId(R.id.inputAmount)).perform(scrollTo(), click(), typeText("48,90"))
        closeSoftKeyboard()

        onView(withId(R.id.inputDate)).perform(scrollTo(), click())
        waitFor(withId(com.google.android.material.R.id.mtrl_picker_header_toggle))
        shot("30_selecteur_date")
        typeDateInPicker(target)
        val expectedDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(target.time)
        waitFor(allOf(withId(R.id.inputDate), withText(expectedDate)))

        onView(withId(R.id.rowCategory)).perform(scrollTo(), click())
        onData(hasToString("Carburant")).inRoot(isDialog()).check(matches(isDisplayed()))
        shot("31_selecteur_categorie")
        onData(hasToString("Carburant")).inRoot(isDialog()).perform(click())
        waitFor(allOf(withId(R.id.tvCategoryValue), withText("Carburant")))

        onView(withId(R.id.inputDescription)).perform(scrollTo(), click(), typeText("Plein fictif"))
        closeSoftKeyboard()
        shot("32_ajout_formulaire_complet")
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())
        // formulaire réinitialisé (iOS) : catégorie revenue à « Choisir une catégorie »
        waitFor(allOf(withId(R.id.tvCategoryValue), withText("Choisir une catégorie")))
    }

    @Test
    fun c02_historique_sections_recherche_filtres() {
        startMain()
        onView(withId(R.id.menu_history)).perform(click())
        waitFor(allOf(withText("Esso Test"), isDisplayed()))
        waitFor(allOf(withId(R.id.tvDate), withText(containsString("Carburant ·")), isDisplayed()))
        shot("33_historique_sections")

        onView(withId(R.id.inputSearch)).perform(click(), typeText("carbu"))
        closeSoftKeyboard()
        waitFor(allOf(withText("Esso Test"), isDisplayed()))
        onView(allOf(withText("Boulangerie Test"), isDisplayed())).check(doesNotExist())
        shot("34_historique_recherche")
        onView(withId(R.id.inputSearch)).perform(clearText())
        closeSoftKeyboard()

        // Filtre : début = aujourd'hui → le ticket Esso (mois précédent) disparaît
        onView(withId(R.id.btnFilter)).perform(click())
        onView(withId(R.id.switchStart)).inRoot(isDialog()).perform(click())
        onView(withId(R.id.tvStart)).inRoot(isDialog()).perform(click())
        waitFor(withId(com.google.android.material.R.id.mtrl_picker_header_toggle))
        typeDateInPicker(Calendar.getInstance())
        shot("35_filtre_dates")
        onView(withText("Appliquer")).inRoot(isDialog()).perform(click())
        waitFor(shown(R.id.tvFilterSummary))
        waitFor(allOf(withText("Boulangerie Test"), isDisplayed()))
        onView(allOf(withText("Esso Test"), isDisplayed())).check(doesNotExist())
        shot("36_historique_filtre_actif")

        onView(withId(R.id.btnFilter)).perform(click())
        onView(withText("Réinitialiser")).inRoot(isDialog()).perform(click())
        waitFor(allOf(withText("Esso Test"), isDisplayed()))
        onView(withId(R.id.tvFilterSummary)).check(matches(not(isDisplayed())))
    }

    @Test
    fun c03_modification_date_et_categorie_libre() {
        startMain()
        onView(withId(R.id.menu_history)).perform(click())
        onView(allOf(withText("Esso Test"), isDisplayed())).perform(click())
        waitFor(inOverlay(R.id.btnEdit))
        onView(inOverlay(R.id.btnEdit)).perform(click())
        waitFor(allOf(inOverlay(R.id.tvCategoryValue), withText("Carburant")))
        onView(inOverlay(R.id.inputAmount)).check(matches(withText("48,90")))
        shot("37_modification_prerempli")

        closeSoftKeyboard()
        onView(inOverlay(R.id.rowCategory)).perform(scrollTo(), click())
        shot("37b_dialogue_categorie_edition")
        onData(hasToString("Autre…")).inRoot(isDialog()).perform(click())
        onView(isAssignableFrom(EditText::class.java)).inRoot(isDialog()).perform(replaceText("Péage fictif"))
        onView(withText("OK")).inRoot(isDialog()).perform(click())
        waitFor(allOf(inOverlay(R.id.tvCategoryValue), withText("Péage fictif")))
        onView(inOverlay(R.id.btnSave)).perform(scrollTo(), click())

        waitFor(allOf(inOverlay(R.id.tvCategory), withText("Péage fictif")))
        shot("38_detail_apres_modification")
        pressBack()

        // La catégorie libre apparaît ensuite dans le sélecteur (iOS : catégories utilisées)
        onView(withId(R.id.menu_add)).perform(click())
        onView(shown(R.id.rowCategory)).perform(scrollTo(), click())
        onData(hasToString("Péage fictif")).inRoot(isDialog()).check(matches(isDisplayed()))
        pressBack()
    }
}
