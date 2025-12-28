package com.etix.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.etix.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs

class MainActivityV2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_v2)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Adapter
        viewPager.adapter = MainPagerAdapter(this)
        viewPager.isUserInputEnabled = true

        // 🎯 Animation iOS-like (PACK 7)
        viewPager.setPageTransformer { page, position ->
            val absPos = abs(position)
            page.alpha = 0.85f + (1 - absPos) * 0.15f
            page.scaleY = 0.95f + (1 - absPos) * 0.05f
            page.translationX = -position * page.width * 0.05f
        }

        // BottomNav → ViewPager
        bottomNav.setOnItemSelectedListener { item ->
            viewPager.currentItem = when (item.itemId) {
                R.id.menu_home -> 0
                R.id.menu_add -> 1
                R.id.menu_history -> 2
                R.id.menu_category -> 3
                R.id.menu_settings -> 4
                else -> 0
            }
            true
        }

        // ViewPager → BottomNav
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.menu.getItem(position).isChecked = true
            }
        })
    }
}
