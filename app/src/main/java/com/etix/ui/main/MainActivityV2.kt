// 📁 com.etix.ui.main.MainActivityV2.kt
package com.etix.ui.main

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.etix.R
import com.etix.adapter.FragmentAdapter
import com.etix.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivityV2 : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {

        session = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(session.getThemeMode())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottomNav)

        // 🔹 ViewPager
        viewPager.adapter = FragmentAdapter(this)
        viewPager.offscreenPageLimit = 4
        viewPager.isUserInputEnabled = true

        // 🔹 BottomNav → ViewPager
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> viewPager.currentItem = 0
                R.id.menu_add -> viewPager.currentItem = 1
                R.id.menu_history -> viewPager.currentItem = 2
                R.id.menu_category -> viewPager.currentItem = 3
                R.id.menu_settings -> viewPager.currentItem = 4
            }
            true
        }

        // 🔹 ViewPager → BottomNav
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.menu.getItem(position).isChecked = true
            }
        })

        // 🔙 Back = retour Home
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
}
