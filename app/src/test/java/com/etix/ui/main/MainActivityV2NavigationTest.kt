package com.etix.ui.main

import android.os.Looper
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.etix.R
import com.etix.fragments.CategoryFragment
import com.etix.fragments.SettingsFragment
import com.etix.ui.add.AddTicketFragmentV2
import com.etix.ui.detail.TicketDetailFragmentV2
import com.etix.ui.detail.TicketEditFragmentV2
import com.etix.ui.history.TicketHistoryFragmentV2
import com.etix.ui.home.HomeFragmentV2
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Lot 1 — navigation principale.
 * Vérifie que chaque onglet affiche le bon écran (et plus l'Accueil par défaut),
 * l'ouverture détail → édition, et le bouton Retour Android.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityV2NavigationTest {

    private fun launch(): ActivityController<MainActivityV2> =
        Robolectric.buildActivity(MainActivityV2::class.java).setup().also { idle() }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun MainActivityV2.pager() = findViewById<ViewPager2>(R.id.viewPager)
    private fun MainActivityV2.nav() = findViewById<BottomNavigationView>(R.id.bottomNav)
    private fun MainActivityV2.overlay() = findViewById<View>(R.id.overlayContainer)

    /** FragmentStateAdapter tague ses fragments "f<itemId>" (itemId = position par défaut). */
    private fun MainActivityV2.page(position: Int) =
        supportFragmentManager.findFragmentByTag("f$position")

    @Test
    fun each_tab_shows_its_own_screen() {
        val activity = launch().get()

        val expected = listOf(
            R.id.menu_home to HomeFragmentV2::class.java,
            R.id.menu_add to AddTicketFragmentV2::class.java,
            R.id.menu_history to TicketHistoryFragmentV2::class.java,
            R.id.menu_category to CategoryFragment::class.java,
            R.id.menu_settings to SettingsFragment::class.java,
        )

        expected.forEachIndexed { index, (menuId, fragmentClass) ->
            activity.nav().selectedItemId = menuId
            idle()
            assertEquals("page affichée pour l'onglet $index", index, activity.pager().currentItem)
            val fragment = activity.page(index)
            assertTrue(
                "onglet $index : attendu ${fragmentClass.simpleName}, obtenu ${fragment?.javaClass?.simpleName}",
                fragmentClass.isInstance(fragment)
            )
        }
    }

    @Test
    fun back_from_other_tab_returns_home_then_exits() {
        val controller = launch()
        val activity = controller.get()

        activity.nav().selectedItemId = R.id.menu_settings
        idle()
        activity.onBackPressedDispatcher.onBackPressed()
        idle()
        assertEquals(MainActivityV2.PAGE_HOME, activity.pager().currentItem)
        assertEquals(R.id.menu_home, activity.nav().selectedItemId)
        assertFalse(activity.isFinishing)

        activity.onBackPressedDispatcher.onBackPressed()
        idle()
        assertTrue("Retour sur l'Accueil doit quitter", activity.isFinishing)
    }

    @Test
    fun detail_then_edit_then_back_stack() {
        val activity = launch().get()
        activity.nav().selectedItemId = R.id.menu_history
        idle()
        assertEquals(View.GONE, activity.overlay().visibility)

        activity.openTicketDetail(42L)
        idle()
        assertEquals(View.VISIBLE, activity.overlay().visibility)
        assertTrue(activity.supportFragmentManager.findFragmentById(R.id.overlayContainer) is TicketDetailFragmentV2)

        activity.findViewById<View>(R.id.btnEdit).performClick()
        idle()
        assertTrue(activity.supportFragmentManager.findFragmentById(R.id.overlayContainer) is TicketEditFragmentV2)

        activity.onBackPressedDispatcher.onBackPressed()
        idle()
        assertTrue(activity.supportFragmentManager.findFragmentById(R.id.overlayContainer) is TicketDetailFragmentV2)

        activity.onBackPressedDispatcher.onBackPressed()
        idle()
        assertEquals(View.GONE, activity.overlay().visibility)
        assertEquals("Retour depuis le détail reste sur Historique", MainActivityV2.PAGE_HISTORY, activity.pager().currentItem)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun switching_tab_closes_open_detail() {
        val activity = launch().get()
        activity.nav().selectedItemId = R.id.menu_history
        idle()
        activity.openTicketDetail(1L)
        idle()

        activity.nav().selectedItemId = R.id.menu_category
        idle()
        assertEquals(View.GONE, activity.overlay().visibility)
        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
        assertEquals(MainActivityV2.PAGE_CATEGORY, activity.pager().currentItem)
    }
}
