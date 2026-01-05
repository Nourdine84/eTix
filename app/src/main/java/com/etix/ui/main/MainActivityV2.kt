package com.etix.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.etix.R
import com.etix.ui.login.LoginActivity
import com.etix.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs

class MainActivityV2 : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        // 🔐 GUARD SESSION
        if (!session.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_main_v2)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottomNav)

        viewPager.adapter = MainPagerAdapter(this)
        viewPager.isUserInputEnabled = true
        viewPager.offscreenPageLimit = 4

        // Animation light (tech only)
        viewPager.setPageTransformer { page, position ->
            val absPos = abs(position)
            page.alpha = 0.85f + (1 - absPos) * 0.15f
            page.scaleY = 0.95f + (1 - absPos) * 0.05f
            page.translationX = -position * page.width * 0.05f
        }

        bottomNav.setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.menu_home -> 0
                R.id.menu_add -> 1
                R.id.menu_history -> 2
                R.id.menu_category -> 3
                R.id.menu_settings -> 4
                else -> 0
            }
            if (viewPager.currentItem != index) {
                viewPager.setCurrentItem(index, true)
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.menu.getItem(position).isChecked = true
            }
        })

        // 🔙 Back press clean
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // 🔒 Re-check session on resume
        if (!session.isLoggedIn()) {
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
