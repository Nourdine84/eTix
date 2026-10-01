package com.etix.ui.main

import android.view.View
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.model.Ticket
import com.etix.testutil.Screens.idle
import com.etix.testutil.TestDb
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

/**
 * Lot 3 — actions non disponibles : visibles mais désactivées, et sans effet (scanner : branché au lot 9).
 * Garde-fou : aucune suppression globale branchée.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProtectedActionsTest {

    private val ctx get() = RuntimeEnvironment.getApplication()

    @Before
    fun seed() {
        TestDb.reset(ctx)
        TestDb.seed(ctx, listOf(
            Ticket(id = 1, store = "Lidl", amount = 10.0, category = "Courses", dateMillis = System.currentTimeMillis()),
            Ticket(id = 2, store = "Esso", amount = 50.0, category = "Transport", dateMillis = System.currentTimeMillis()),
        ))
    }

    private fun ticketCount() = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first().size }

    @Test
    fun clear_all_button_is_disabled_and_deletes_nothing() {
        val a = Robolectric.buildActivity(MainActivityV2::class.java).setup().get(); idle()
        a.openSettings(); idle()
        val btn = a.findViewById<View>(R.id.btnClearAll)
        assertFalse("« Vider tous les tickets » doit être désactivé", btn.isEnabled)
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.tvClearAllUnavailable).visibility)
        btn.performClick(); idle()
        assertEquals(2, ticketCount())
    }

    @Test
    @Config(qualifiers = "+night")
    fun clear_all_button_is_disabled_in_dark_layout_too() {
        val a = Robolectric.buildActivity(MainActivityV2::class.java).setup().get(); idle()
        a.openSettings(); idle()
        // variante layout-night : identifiant historique différent
        assertFalse(a.findViewById<View>(R.id.btnDeleteDatabase).isEnabled)
        assertEquals(2, ticketCount())
    }

    /** Lot 9 : scanner branché (décision produit) — ouvrir puis annuler le parcours ne crée aucun ticket. */
    @Test
    fun scan_buttons_open_the_scan_flow_without_creating_tickets() {
        val a = Robolectric.buildActivity(MainActivityV2::class.java).setup().get(); idle()
        val home = a.supportFragmentManager.findFragmentByTag("f0")!!.requireView()
        assertTrue(home.findViewById<View>(R.id.btnScanTicket).isEnabled)
        home.findViewById<View>(R.id.btnScanTicket).performClick(); idle()
        assertTrue(a.supportFragmentManager.findFragmentById(R.id.overlayContainer) is com.etix.ui.scan.ScanFlowFragment)
        assertEquals(MainActivityV2.PAGE_ADD, a.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).currentItem)
        a.findViewById<View>(R.id.btnScanCancel).performClick(); idle()
        assertEquals(null, a.supportFragmentManager.findFragmentById(R.id.overlayContainer))
        val add = a.supportFragmentManager.findFragmentByTag("f1")!!.requireView()
        assertTrue(add.findViewById<View>(R.id.btnScanTicket).isEnabled)
        assertEquals(2, ticketCount())
    }
}
