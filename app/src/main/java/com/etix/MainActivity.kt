package com.etix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.etix.adapter.FragmentAdapter
import com.etix.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewPager = binding.viewPager
        val bottomNav = binding.bottomNavigationView

        viewPager.adapter = FragmentAdapter(this)
        viewPager.isUserInputEnabled = true

        // Menu → Page
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

        // Swipe → Menu
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.menu.getItem(position).isChecked = true
            }
        })
    }
}