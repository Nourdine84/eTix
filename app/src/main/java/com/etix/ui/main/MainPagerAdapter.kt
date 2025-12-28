package com.etix.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.etix.ui.add.AddTicketFragmentV2
import com.etix.ui.category.CategoryFragmentV2
import com.etix.ui.history.TicketHistoryFragmentV2
import com.etix.ui.home.HomeFragmentV2
import com.etix.ui.settings.SettingsFragmentV2

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragmentV2()
            1 -> AddTicketFragmentV2()
            2 -> TicketHistoryFragmentV2()
            3 -> CategoryFragmentV2()
            4 -> SettingsFragmentV2()
            else -> HomeFragmentV2()
        }
    }
}
