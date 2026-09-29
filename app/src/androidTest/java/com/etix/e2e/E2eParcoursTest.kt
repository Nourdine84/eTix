package com.etix.e2e

import android.content.Intent
import android.widget.EditText
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.R
import com.etix.SplashActivity
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.ui.main.MainActivityV2
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Parcours de bout en bout sur émulateur, données FICTIVES, app installée à neuf.
 * Méthodes ordonnées : l'état (session, tickets) passe d'une méthode à la suivante.
 * La persistance après arrêt forcé du processus est vérifiée par E2ePersistanceTest (lancé après `am force-stop`).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eParcoursTest {

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor(withId(R.id.bottomNav))
    }

    private fun tab(id: Int) = onView(withId(id)).perform(click())
    private fun shown(id: Int) = allOf(withId(id), isDisplayed())
    private fun inOverlay(id: Int) = allOf(withId(id), isDescendantOfA(withId(R.id.overlayContainer)))

    @Test
    fun a01_premier_lancement_onboarding_connexion() {
        ctx.startActivity(Intent(ctx, SplashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor(withId(R.id.btnStart))
        shot("01_onboarding")
        onView(withId(R.id.btnStart)).perform(click())

        waitFor(withId(R.id.inputUsername))
        shot("02_connexion")
        onView(withId(R.id.inputUsername)).perform(typeText("Testeur"))
        onView(withId(R.id.inputPassword)).perform(typeText("fictif"))
        closeSoftKeyboard()
        onView(withId(R.id.btnLogin)).perform(click())

        waitFor(withId(R.id.bottomNav))
        waitFor(withText("Aucun ticket enregistré"))
        shot("03_accueil_vide")
    }

    @Test
    fun a02_etats_vides_et_actions_indisponibles() {
        startMain()
        onView(shown(R.id.btnScanTicket)).check(matches(not(isEnabled())))

        tab(R.id.menu_add)
        waitFor(withId(R.id.inputStore))
        onView(shown(R.id.btnScanTicket)).check(matches(not(isEnabled())))
        shot("04_ajouter_vide")

        tab(R.id.menu_history)
        waitFor(withId(R.id.emptyState))
        shot("05_historique_vide")

        tab(R.id.menu_category)
        shot("06_categories_vide")

        tab(R.id.menu_stores)
        waitFor(withId(R.id.emptyStores))
        shot("07_magasins_vide")

        tab(R.id.menu_home)
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnClearAll)).check(matches(not(isEnabled())))
        onView(withId(R.id.btnClearAll)).perform(click()) // sans effet attendu
        shot("08_reglages")
        pressBack()
        waitFor(withId(R.id.tvTicketCount))
    }

    @Test
    fun a03_ajout_ticket_montant_a_virgule_au_clavier() {
        startMain()
        tab(R.id.menu_add)
        onView(shown(R.id.inputStore)).perform(click(), typeText("Boulangerie Test"))
        onView(shown(R.id.inputAmount)).perform(click(), typeText("12,50"))
        shot("09_ajout_clavier_ouvert")

        // Ce que le clavier a réellement laissé passer dans le champ
        var saisi = ""
        onView(shown(R.id.inputAmount)).check { v, _ -> saisi = (v as EditText).text.toString() }
        closeSoftKeyboard()
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())

        tab(R.id.menu_home)
        waitFor(withText("1 ticket enregistré"))
        shot("10_accueil_un_ticket")
        assertEquals("Saisie clavier du montant « 12,50 »", "12,50", saisi)
        waitFor(allOf(withId(R.id.tvHeroAmount), withText("12,50 €")))
    }

    @Test
    fun a04_historique_detail_modification_retour() {
        startMain()
        tab(R.id.menu_history)
        waitFor(withText("Boulangerie Test"))
        shot("11_historique")
        onView(allOf(withText("Boulangerie Test"), isDisplayed())).perform(click())

        waitFor(inOverlay(R.id.btnEdit))
        shot("12_detail")
        onView(inOverlay(R.id.btnEdit)).perform(click())

        waitFor(inOverlay(R.id.btnSave))
        onView(inOverlay(R.id.inputAmount)).perform(clearText(), typeText("15,75"))
        closeSoftKeyboard()
        shot("13_modification")
        onView(inOverlay(R.id.btnSave)).perform(click())

        waitFor(allOf(inOverlay(R.id.tvAmount), withText("15,75 €")))
        shot("14_detail_apres_modification")
        pressBack()
        waitFor(allOf(withText("15,75 €"), isDisplayed()))
        onView(withId(R.id.overlayContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        shot("15_historique_apres_retour")
    }

    @Test
    fun a05_magasins_periodes_et_montants() {
        startMain()
        tab(R.id.menu_stores)
        waitFor(allOf(withId(R.id.tvStoreName), withText("Boulangerie Test")))
        onView(allOf(withId(R.id.tvStoreTotal), isDisplayed())).check(matches(withText("15,75 €")))
        shot("16_magasins_mois")

        onView(withId(R.id.btnPeriodToday)).perform(click())
        waitFor(allOf(withId(R.id.tvStoreTotal), withText("15,75 €")))
        shot("17_magasins_aujourdhui")
        onView(withId(R.id.btnPeriodYear)).perform(click())
        waitFor(allOf(withId(R.id.tvStoreTotal), withText("15,75 €")))

        onView(allOf(withId(R.id.tvStoreName), withText("Boulangerie Test"))).perform(click())
        waitFor(allOf(withId(R.id.tvDetailTotal), withText("15,75 €")))
        shot("18_fiche_magasin")
        pressBack()
        waitFor(withId(R.id.recyclerStores))
    }

    @Test
    fun a06_clavier_et_defilement() {
        startMain()
        onView(withId(R.id.homeScroll)).perform(swipeUp())
        waitFor(shown(R.id.btnHistory))
        shot("19_accueil_defile")

        tab(R.id.menu_add)
        onView(shown(R.id.inputStore)).perform(click())
        // Clavier ouvert : le bouton est masqué ; il doit rester atteignable par défilement
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo()).check(matches(isDisplayed()))
        shot("20_ajouter_clavier")
        closeSoftKeyboard()
    }

    @Test
    fun a07_theme_sombre_puis_clair() {
        startMain()
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnToggleTheme)).perform(click())   // → sombre (recréation)
        waitFor(withId(R.id.btnToggleTheme))
        shot("21_reglages_sombre")
        pressBack()
        waitFor(withId(R.id.tvTicketCount))
        shot("22_accueil_sombre")
        tab(R.id.menu_history)
        waitFor(withText("Boulangerie Test"))
        shot("23_historique_sombre")
        tab(R.id.menu_stores)
        waitFor(allOf(withId(R.id.tvStoreName), withText("Boulangerie Test")))
        shot("24_magasins_sombre")
        tab(R.id.menu_add)
        shot("25_ajouter_sombre")

        tab(R.id.menu_home)
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.btnToggleTheme))
        onView(withId(R.id.btnToggleTheme)).perform(click())   // → clair
        waitFor(withId(R.id.btnToggleTheme))
        shot("26_reglages_clair")
    }
}
