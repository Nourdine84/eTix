package com.etix.ui.main

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.etix.R
import com.etix.fragments.FragmentAdapter
import com.etix.ui.detail.TicketDetailFragmentV2
import com.etix.ui.detail.TicketEditFragmentV2
import com.etix.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivityV2 : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var overlay: FrameLayout
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {

        session = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(session.getThemeMode())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_v2)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottomNav)
        overlay = findViewById(R.id.overlayContainer)

        viewPager.adapter = FragmentAdapter(this)
        viewPager.offscreenPageLimit = 4
        viewPager.isUserInputEnabled = true

        bottomNav.setOnItemSelectedListener { item ->
            val page = when (item.itemId) {
                R.id.menu_home -> PAGE_HOME
                R.id.menu_add -> PAGE_ADD
                R.id.menu_history -> PAGE_HISTORY
                R.id.menu_category -> PAGE_CATEGORY
                R.id.menu_settings -> PAGE_SETTINGS
                else -> return@setOnItemSelectedListener false
            }
            goToPage(page)
            true
        }

        // Re-tap sur l'onglet courant : referme un détail ouvert (comportement TabBar iOS)
        bottomNav.setOnItemReselectedListener { clearOverlay() }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.menu.getItem(position).isChecked = true
            }
        })

        // L'overlay n'est visible que s'il contient un écran poussé
        supportFragmentManager.addOnBackStackChangedListener { syncOverlayVisibility() }
        syncOverlayVisibility()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    // 1. Écran poussé (détail / édition) → on dépile
                    supportFragmentManager.backStackEntryCount > 0 ->
                        supportFragmentManager.popBackStack()

                    // 2. Autre onglet → retour Accueil
                    viewPager.currentItem != PAGE_HOME ->
                        goToPage(PAGE_HOME)

                    // 3. Accueil → comportement système (quitter)
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }

    // ✅ API centrale de navigation entre onglets
    fun goToPage(index: Int) {
        clearOverlay()
        viewPager.setCurrentItem(index, false)
    }

    // ✅ API centrale : écrans poussés au-dessus des onglets
    fun openTicketDetail(ticketId: Long) {
        push(TicketDetailFragmentV2.newInstance(ticketId), BACKSTACK_DETAIL)
    }

    fun openTicketEdit(ticketId: Long) {
        push(TicketEditFragmentV2.newInstance(ticketId), BACKSTACK_EDIT)
    }

    /** Ferme l'édition ET le détail (ex. après suppression du ticket). */
    fun closeTicketFlow() {
        supportFragmentManager.popBackStack(
            BACKSTACK_DETAIL,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    private fun push(fragment: Fragment, name: String) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )
            .replace(R.id.overlayContainer, fragment)
            .addToBackStack(name)
            .commit()
    }

    private fun clearOverlay() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }

    private fun syncOverlayVisibility() {
        overlay.visibility =
            if (supportFragmentManager.backStackEntryCount > 0) View.VISIBLE else View.GONE
    }

    companion object {
        const val PAGE_HOME = 0
        const val PAGE_ADD = 1
        const val PAGE_HISTORY = 2
        const val PAGE_CATEGORY = 3
        const val PAGE_SETTINGS = 4

        private const val BACKSTACK_DETAIL = "ticket_detail"
        private const val BACKSTACK_EDIT = "ticket_edit"
    }
}
