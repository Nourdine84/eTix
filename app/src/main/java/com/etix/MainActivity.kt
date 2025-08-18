package com.etix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.etix.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔗 On lie l’activity à son layout principal
        setContentView(R.layout.activity_home)

        // 🔍 Référence aux éléments du layout
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // 🧩 Adaptateur pour nos fragments (ViewPager2)
        val fragmentAdapter = FragmentAdapter(this)
        viewPager.adapter = fragmentAdapter

        // 🔁 Gestion du swipe : synchronisation avec le menu
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigation.menu.getItem(position).isChecked = true
            }
        })

        // 🎯 Clics sur les éléments du menu
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> viewPager.currentItem = 0
                R.id.menu_add -> viewPager.currentItem = 1
                R.id.menu_history -> viewPager.currentItem = 2
                R.id.menu_categories -> viewPager.currentItem = 3
                R.id.menu_settings -> viewPager.currentItem = 4
            }
            true
        }
    }
}
