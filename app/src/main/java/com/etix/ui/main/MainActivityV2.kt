package com.etix.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.etix.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivityV2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_v2)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        viewPager.adapter = MainPagerAdapter(this)
        viewPager.isUserInputEnabled = true

        bottomNav.setOnItemSelectedListener {
            viewPager.currentItem = when (it.itemId) {
                R.id.menu_home -> 0
                R.id.menu_add -> 1
                R.id.menu_history -> 2
                R.id.menu_category -> 3
                R.id.menu_settings -> 4
                else -> 0
            }
            true
        }


        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.menu.getItem(position).isChecked = true
            }
        })
    }
}
