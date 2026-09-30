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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Lot 3 — actions non disponibles : visibles mais désactivées, et sans effet.
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

    @Test
    fun scan_buttons_are_disabled_everywhere() {
        val a = Robolectric.buildActivity(MainActivityV2::class.java).setup().get(); idle()
        val home = a.supportFragmentManager.findFragmentByTag("f0")!!.requireView()
        assertFalse(home.findViewById<View>(R.id.btnScanTicket).isEnabled)
        val add = a.supportFragmentManager.findFragmentByTag("f1")!!.requireView()
        assertFalse(add.findViewById<View>(R.id.btnScanTicket).isEnabled)
    }
}
