package com.etix.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.etix.fragments.AddTicketFragment
import com.etix.fragments.CategoryFragment
import com.etix.fragments.HomeFragment
import com.etix.fragments.SettingsFragment
import com.etix.fragments.TicketHistoryFragment

class FragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> AddTicketFragment()
            2 -> TicketHistoryFragment()
            3 -> CategoryFragment()
            4 -> SettingsFragment()
            else -> HomeFragment()
        }
    }
}
