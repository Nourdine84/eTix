package com.etix.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.etix.ui.home.HomeFragmentV2
import com.etix.ui.add.AddTicketFragmentV2
import com.etix.ui.history.TicketHistoryFragmentV2

class FragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragmentV2()
            1 -> AddTicketFragmentV2()
            2 -> TicketHistoryFragmentV2()
            // CategoryFragmentV2 / SettingsFragmentV2 sont des placeholders vides (empty_layout) :
            // on branche les versions V1 fonctionnelles en attendant leur refonte V2.
            3 -> CategoryFragment()
            4 -> SettingsFragment()
            else -> HomeFragmentV2()
        }
    }
}
