package com.etix.e2e

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.R
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.e2e.ScanE2e.stubPicker
import com.etix.e2e.ScanE2e.ticketImage
import com.etix.e2e.ScanE2e.tickets
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Lot 9 — parcours de scan sur émulateur en français (fr-FR) : ML Kit réel, sélecteur d'image simulé.
 * Vérifie la date préremplie telle qu'affichée (« 12 janv. 2026 ») et enregistrée (12/01/2026, jour/mois),
 * le montant à virgule et les libellés. Données fictives ; un ticket créé après « Enregistrer ».
 */
@RunWith(AndroidJUnit4::class)
class E2eScanFrTest {

    @Before fun setUp() {
        assertEquals("Émulateur attendu en français", "fr", Locale.getDefault().language)
        SessionManager(ctx).apply { markFirstLaunchDone(); login("Testeur scan FR") }
        Intents.init()
    }

    @After fun tearDown() = Intents.release()

    @Test fun scan_en_francais_date_preremplie_puis_enregistrement() {
        val before = tickets()
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eScanFrTest"); waitFor(withId(R.id.bottomNav))
        onView(withId(R.id.menu_add)).perform(click())
        waitFor(withId(R.id.inputStore))
        onView(allOf(withId(R.id.btnScanTicket), isDisplayed())).perform(click())
        waitFor(withText("Prendre une photo"))
        stubPicker(ticketImage("fr.png"))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)

        val jan12 = Calendar.getInstance().apply { clear(); set(2026, Calendar.JANUARY, 12) }
        val expected = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRANCE).format(jan12.time)
        assertTrue("format français attendu, obtenu « $expected »", expected.contains("janv") && expected.contains("2026"))
        onView(withId(R.id.inputDate)).check(matches(withText(expected)))
        onView(withId(R.id.inputStore)).check(matches(withText("ESSO")))
        onView(withId(R.id.inputAmount)).check(matches(withText("23,45")))
        onView(withId(R.id.badgeDate)).check(matches(withText("Détectée")))
        shot("f20_scan_formulaire_prerempli_fr")
        assertEquals("aucun ticket avant « Enregistrer »", before, tickets())

        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())
        val end = System.currentTimeMillis() + 10_000
        while (tickets().size == before.size && System.currentTimeMillis() < end) Thread.sleep(200)
        val after = tickets()
        assertEquals(before.size + 1, after.size)
        val t = after.last()
        val c = Calendar.getInstance().apply { timeInMillis = t.dateMillis }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
        assertEquals(2026, c.get(Calendar.YEAR))
        assertEquals(23.45, t.amount, 0.001)
    }
}
